package io.pleo.antaeus.core.exceptions

class RetryExceededException(invoiceId: Int, retryCount: Int) :
    Exception("Aborting operation for invoice '$invoiceId as current retry count '$retryCount' exceeded Max Retry count")
