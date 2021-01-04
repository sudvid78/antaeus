package io.pleo.antaeus.core.exceptions

class LowBalanceException(invoiceId: Int, customerId: Int) :
    Exception("Customer '$customerId' balance is low for Invoide '$invoiceId")
