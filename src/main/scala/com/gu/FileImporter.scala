package com.gu

import kantan.csv._
import kantan.csv.ops._
import kantan.csv.generic._
import kantan.csv.joda.time._
import java.io.File

import com.typesafe.scalalogging.LazyLogging
import org.joda.time.LocalDate
import org.joda.time.format.DateTimeFormat

object FileImporter extends LazyLogging {
  val format = DateTimeFormat.forPattern("dd/MM/yyyy")
  implicit val decoder: CellDecoder[LocalDate] = localDateDecoder(format)

  case class Invoice(
                      `Invoice.Id`: String,
                      `Invoice.InvoiceNumber`: String,
                      `Invoice.Status`: String,
                      `Invoice.SourceId`: String, // billRun
  )

  def importCsv(filename: String = "subs.csv"): List[ReadResult[Invoice]] =
    new File(filename).asCsvReader[Invoice](rfc.withHeader).toList

}
