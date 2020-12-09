/*
    Implements endpoints related to invoices.
 */

package io.pleo.antaeus.core.services

import io.pleo.antaeus.core.exceptions.InvoiceNotFoundException
import io.pleo.antaeus.data.AntaeusDal
import io.pleo.antaeus.models.Invoice

class InvoiceService(private val dal: AntaeusDal) {
    fun fetchAll(): List<Invoice> {
        return dal.fetchInvoices()
    }

    fun fetch(id: Int): Invoice {
        return dal.fetchInvoice(id) ?: throw InvoiceNotFoundException(id)
    }
    //fetch the Invoice record with status = Pending
    fun fetchPendingInvoices(timeZone: String): List<Invoice> {
        return dal.fetchPendingInvoices(timeZone)
    }

    //update Invoice status
    fun updateInvoiceStatus(customerId:Int,statusTo:String){
        return dal.updateInvoicesStatus(customerId,statusTo)
    }
}
