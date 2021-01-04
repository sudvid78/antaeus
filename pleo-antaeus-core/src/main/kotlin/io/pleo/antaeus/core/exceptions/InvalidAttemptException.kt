package io.pleo.antaeus.core.exceptions

class InvalidAttemptException(invoiceId: Int, customerId: Int) :
    Exception("Customer of invoice '$invoiceId' been already charged. Invalid Attempt")
