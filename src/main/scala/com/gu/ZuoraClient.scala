package com.gu

import com.gu.FileImporter.Invoice
import com.typesafe.scalalogging.LazyLogging
import org.joda.time.LocalDate
import org.joda.time.format.DateTimeFormat
import scalaj.http.{BaseHttp, Http, HttpOptions}
import org.json4s._
import org.json4s.native.JsonMethods._
import org.json4s.native.Serialization.write

object LocalDateSerializer extends CustomSerializer[LocalDate](format => ({
  case JString(str) => LocalDate.parse(str)
  case JNull => null
}, {
  case value: LocalDate  =>
    val formatter = DateTimeFormat.forPattern("YYYY-MM-dd")
    JString(formatter.print(value))
}
))

trait ZuoraJsonFormats {
  implicit val codec = DefaultFormats ++ List(LocalDateSerializer)
}

object ZuoraClient extends ZuoraJsonFormats with LazyLogging {
  import ZuoraOauth._
  import ZuoraHostSelector._

  object HttpWithLongTimeout extends BaseHttp(
    options = Seq(
      HttpOptions.connTimeout(5000),
      HttpOptions.readTimeout(30000),
      HttpOptions.followRedirects(false)
    )
  )

  case class CancelInvoice(Status: String)
  case class CancelInvoiceResponse(Success: Boolean, Id: String)

  // https://www.zuora.com/developer/api-reference/#operation/Object_PUTInvoice
  def cancelInvoice(invoice: Invoice): CancelInvoiceResponse = {
    val response = HttpWithLongTimeout(s"$host/v1/object/invoice/${invoice.`Invoice.Id`}")
      .header("Authorization", s"Bearer $accessToken")
      .header("content-type", "application/json")
      .postData(write(CancelInvoice("Canceled")))
      .method("PUT")
      .asString

    response.code match {
      case 200 => parse(response.body).extract[CancelInvoiceResponse]
      case _ => throw new RuntimeException(s"Failed to cancel invoice $invoice due to Zuora networking issue: $response")
    }
  }

  case class DeleteInvoice(Status: String)
  case class ZuoraError(Code: String, Message: String)
  case class DeleteInvoiceResponse(success: Boolean, id: String)
  case class DeleteInvoiceErrorResponse(success: Boolean, id: String, errors: List[ZuoraError])

  // https://www.zuora.com/developer/api-reference/#operation/Object_DELETEInvoice
  def deleteInvoice(invoice: Invoice): DeleteInvoiceResponse = {
    val response = HttpWithLongTimeout(s"$host/v1/object/invoice/${invoice.`Invoice.Id`}")
      .header("Authorization", s"Bearer $accessToken")
      .method("DELETE")
      .asString

    response.code match {
      case 200 => parse(response.body).extract[DeleteInvoiceResponse]
      case 400 =>
        val body = parse(response.body).extract[DeleteInvoiceErrorResponse]
        if (body.errors.length == 1 && body.errors.head.Code == "CANNOT_DELETE" && body.errors.head.Message == "invalid id")
          // already deleted
          DeleteInvoiceResponse(true, body.id)
        else
          throw new RuntimeException(
            s"Failed to delete invoice $invoice due to Zuora networking issue: $response"
          )
      case _ => throw new RuntimeException(s"Failed to delete invoice $invoice due to Zuora networking issue: $response")
    }
  }
}

case class Token(
  access_token: String,
  token_type: String,
  expires_in: String,
  scope: String,
  jti: String
)

// https://www.zuora.com/developer/api-reference/#operation/createToken
object ZuoraOauth extends ZuoraJsonFormats {
  import java.util.{Timer, TimerTask}
  import ZuoraHostSelector._

  var accessToken: String = null

  private def getAccessToken(): String = {
    val response = Http(s"$host/oauth/token")
      .postForm(Seq(
        "client_id" -> Config.Zuora.client_id,
        "client_secret" -> Config.Zuora.client_secret,
        "grant_type" -> "client_credentials"
      ))
      .asString

    response.code match {
      case 200 => parse(response.body).extract[Token].access_token
      case _ => throw new RuntimeException(s"Failed to authenticate with Zuora: $response")
    }
  }

  private val timer = new Timer()

  timer.schedule(
    new TimerTask { def run(): Unit = accessToken = getAccessToken() },
    0, 1 * 60 * 1000 // refresh token every 1 min
  )
  accessToken = getAccessToken() // set token on initialization
}

object ZuoraHostSelector {
  val host: String =
    Config.Zuora.stage match {
      case "DEV" | "dev" => "https://rest.apisandbox.zuora.com"
      case "PROD" | "prod" => "https://rest.zuora.com"
      case _ => "https://rest.apisandbox.zuora.com"
    }
}

