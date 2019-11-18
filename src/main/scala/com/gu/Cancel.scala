package com.gu

import com.typesafe.scalalogging.LazyLogging

import scala.util.Try

/**
 * Import file should be generated with:
 * "query" : "select Invoice.Id, Invoice.InvoiceNumber, Invoice.Status, Invoice.sourceId from invoiceitem where Invoice.sourceId = 'BR-00010739' and Invoice.Status = 'Draft' and servicestartdate >= '2019-11-16'",
 */
object Cancel extends App with LazyLogging {
  if (args.length == 0)
    Abort("Please provide import filename")
  val filename = args(0)
  val resumeSubscriptionNameOpt = Try(args(1)).toOption
  val csvImport = FileImporter.importCsv(filename)

  val importSize = csvImport.size
  var successfullyCancelledCount = 0

  logger.info(s"Start cancelling $importSize invoices from $filename...")
  ResumeProcessing(csvImport, resumeSubscriptionNameOpt).foreach {
    case Left(importError) =>
      Abort(s"Bad import file: $importError")

    case Right(invoice) =>
      if (!(invoice.`Invoice.Status` == "Draft" && invoice.`Invoice.SourceId` == "BR-00010739"))
        Abort("Bad import file. Export with: select Invoice.Id, Invoice.InvoiceNumber, Invoice.Status, Invoice.sourceId from invoiceitem where Invoice.sourceId = 'BR-00010739' and Invoice.Status = 'Draft' and servicestartdate >= '2019-11-16'")

//      logger.info(s"woohoo $invoice")

      try {
        val cancelInvoiceResponse = ZuoraClient.cancelInvoice(invoice)
        if (cancelInvoiceResponse.Success) {
          successfullyCancelledCount = successfullyCancelledCount + 1
          logger.info(s"$successfullyCancelledCount - ${invoice.`Invoice.InvoiceNumber`} successfully cancelled")
        } else {
          logger.error(
            s"Failed to cancel invoice $invoice. Fix and resume from ${invoice.`Invoice.InvoiceNumber`}: " +
            s"$cancelInvoiceResponse"
          )
        }
      } catch {
        case e: Throwable =>
          logger.error(
            s"Failed to cancel invoice $invoice. Fix and resume from ${invoice.`Invoice.InvoiceNumber`}",
            e
          )
      }
  }

  logger.info(s"Results")
  logger.info("==========================================================")
  logger.info(s"Import size: $importSize")
  logger.info(s"Successfully cancelled count: $successfullyCancelledCount")
  logger.info(Console.GREEN + s"DONE.")
}
