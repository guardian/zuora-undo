package com.gu

import com.typesafe.scalalogging.LazyLogging

import scala.util.Try

/**
 * Import file should be generated with:
 * "query" : "select Invoice.Id, Invoice.InvoiceNumber, Invoice.Status, Invoice.sourceId from invoiceitem where Invoice.sourceId = 'BR-00010739' and Invoice.Status = 'Cancelled'",
 */
object Delete extends App with LazyLogging {
  if (args.length == 0)
    Abort("Please provide import filename")
  val filename = args(0)
  val resumeSubscriptionNameOpt = Try(args(1)).toOption
  val csvImport = FileImporter.importCsv(filename)

  val importSize = csvImport.size
  var successfullyDeletedCount = 0

  logger.info(s"Start deleting $importSize invoices from $filename...")
  ResumeProcessing(csvImport, resumeSubscriptionNameOpt).foreach {
    case Left(importError) =>
      Abort(s"Bad import file: $importError")

    case Right(invoice) =>
      if (!(invoice.`Invoice.Status` == "Cancelled" && invoice.`Invoice.SourceId` == "BR-00010739"))
        Abort("Bad import file. Export with: select Invoice.Id, Invoice.InvoiceNumber, Invoice.Status, Invoice.sourceId from invoiceitem where Invoice.sourceId = 'BR-00010739' and Invoice.Status = 'Cancelled'")

      try {
        val deleteInvoiceResponse = ZuoraClient.deleteInvoice(invoice)
        if (deleteInvoiceResponse.success) {
          successfullyDeletedCount = successfullyDeletedCount + 1
          logger.info(
            s"$successfullyDeletedCount - ${invoice.`Invoice.InvoiceNumber`} successfully deleted"
          )
        } else {
          logger.error(
            s"Failed to delete invoice $invoice. Fix and resume from ${invoice.`Invoice.InvoiceNumber`}: $deleteInvoiceResponse"
          )
        }
      } catch {
        case e: Throwable =>
          logger.error(
            s"Failed to delete invoice $invoice. Fix and resume from ${invoice.`Invoice.InvoiceNumber`}",
            e
          )
      }
  }

  logger.info(s"Results")
  logger.info("==========================================================")
  logger.info(s"Import size: $importSize")
  logger.info(s"Successfully deleted count: $successfullyDeletedCount")
  logger.info(Console.GREEN + s"DONE.")
}
