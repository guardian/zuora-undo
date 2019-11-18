# zuora-undo

You have to first cancel all the draft invoices before you can delete them.

## To cancel

The 'Cancel' app depends on a CSV in the following format

```
2c92a06737383uejuedjklewljk,INV01111111,Draft,BR-00010739
r392306737383uejuedjklewljk,INV01111112,Draft,BR-00010739
...
```


### How to run

1. Get the csv export

    ```
    POST /v1/batch-query/ HTTP/1.1
    Host: rest.zuora.com
    Accept: application/json
    Content-Type: application/json
    Authorization: Bearer ************

    {
        "format" : "csv",
        "version" : "1.1",
        "name" : "fix-accidental-billrun",
        "encrypted" : "none",
        "useQueryLabels" : "true",
        "dateTimeUtc" : "true",
        "queries" : [
            {
                "name" : "Mario-InvoiceItemAfter16Nov",
                "query" : "select Invoice.Id, Invoice.InvoiceNumber, Invoice.Status, Invoice.sourceId from invoiceitem where Invoice.sourceId = 'BR-00010739' and Invoice.Status = 'Draft' and servicestartdate >= '2019-11-16'",
                "type" : "zoqlexport"
            }
        ]
    }
    ```

1. Deduplicate csv file

    ```
    sort -u Mario-InvoiceItemAfter16Nov.csv > input-dedup.csv
    ```

1. Put zuora credentials in `application.conf` 

    ```
    zuora {
      stage = PROD
      client_id = ********** 
      client_secret = *********** 
    }
    ```
1. Drop the import file at project root and run `Cancel.scala` with:

    ```
    run input-dedup.csv
    ```
1. To resume from particular invoice

    ```
    run input-dedup.csv INV01111112 
    ``` 
    
    
## To delete

The 'Delete' app depends on a CSV in the following format

```
2c92a06737383uejuedjklewljk,INV01111111,Cancelled,BR-00010739
r392306737383uejuedjklewljk,INV01111112,Cancelled,BR-00010739
...
```


### How to run

1. Get the csv export

    ```
    POST /v1/batch-query/ HTTP/1.1
    Host: rest.zuora.com
    Accept: application/json
    Content-Type: application/json
    Authorization: Bearer ************

    {
        "format" : "csv",
        "version" : "1.1",
        "name" : "fix-accidental-billrun",
        "encrypted" : "none",
        "useQueryLabels" : "true",
        "dateTimeUtc" : "true",
        "queries" : [
            {
                "name" : "Mario-InvoiceItemAfter16Nov",
                "query" : "select Invoice.Id, Invoice.InvoiceNumber, Invoice.Status, Invoice.sourceId from invoiceitem where Invoice.sourceId = 'BR-00010739' and Invoice.Status = 'Cancelled'",
                "type" : "zoqlexport"
            }
        ]
    }
    ```

1. Deduplicate csv file

    ```
    sort -u Mario-InvoiceItemAfter16Nov.csv > input-dedup.csv
    ```

1. Drop the import file at project root and run `Delete.scala` with:

    ```
    run input-dedup.csv
    ```
1. To resume from particular invoice

    ```
    run input-dedup.csv INV01111112 
    ``` 
    
