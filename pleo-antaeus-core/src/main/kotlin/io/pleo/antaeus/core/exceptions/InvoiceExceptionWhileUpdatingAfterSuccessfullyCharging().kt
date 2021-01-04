package io.pleo.antaeus.core.exceptions

class InvoiceExceptionWhileCharging(id: Int) :
        Exception("Customer of invoice '$id' been already charged. Invalid Attempt")

