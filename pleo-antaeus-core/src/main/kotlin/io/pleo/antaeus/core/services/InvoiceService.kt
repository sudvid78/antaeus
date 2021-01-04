/*
    Implements endpoints related to invoices.
 */

package io.pleo.antaeus.core.services

import io.pleo.antaeus.core.exceptions.InvoiceNotFoundException
import io.pleo.antaeus.data.AntaeusDal
import io.pleo.antaeus.data.InvoiceTable
import io.pleo.antaeus.models.Invoice
import io.pleo.antaeus.models.InvoiceStatus

class InvoiceService(private val dal: AntaeusDal) {
    fun fetchAll(): List<Invoice> {
        return dal.fetchInvoices()
    }

    fun fetch(id: Int): Invoice {
        return dal.fetchInvoice(id) ?: throw InvoiceNotFoundException(id)
    }
    //fetch the Invoice record with status = Pending
    fun fetchPendingInvoices(offsetId: Int, batchSize: Int,invoiceStatus:InvoiceStatus,timezone: String): List<Invoice> {
        return dal.fetchPendingInvoices(offsetId,batchSize,invoiceStatus,timezone)
    }

    //update Invoice status
    fun updateInvoice(id:Int,statusTo:InvoiceStatus,idemPotencyKey:String?=null,retryCount:Int=-1):Invoice?{
        return dal.updateInvoice(id,statusTo,idemPotencyKey,retryCount) ?: throw InvoiceNotFoundException(id)
    }

    fun updateInvoiceIdempotencyKey(id:Int,idempotencyKey:String?=null){
        return dal.updateInvoiceIdempotencyKey(id,idempotencyKey)
    }
}
