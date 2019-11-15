package com.gu

import com.gu.FileImporter.Invoice
import com.typesafe.scalalogging.LazyLogging
import kantan.csv.ReadResult

object ResumeProcessing extends LazyLogging{
  def apply(
             csvImport: List[ReadResult[Invoice]],
             resumeInvoiceNumberOpt: Option[String]
  ): List[ReadResult[Invoice]] = {

    resumeInvoiceNumberOpt match {
      case None =>
        csvImport

      case Some(resumeInvoiceNumber) =>
        val resumeIndex = csvImport.indexWhere { priceRiseRecord =>
          priceRiseRecord match {
            case Left(_) => false
            case Right(invoice) => invoice.`Invoice.InvoiceNumber` == resumeInvoiceNumber
          }
        }

        assert(resumeIndex != -1, "InvoiceNumber must exists in import file to resume processing")

        logger.info(s"Resuming processing from $resumeInvoiceNumber...")
        csvImport.drop(resumeIndex)
    }
  }

}
