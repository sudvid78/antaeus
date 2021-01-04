/*
    Implements the data access layer (DAL).
    The data access layer generates and executes requests to the database.

    See the `mappings` module for the conversions between database rows and Kotlin objects.
 */

package io.pleo.antaeus.data

import io.pleo.antaeus.models.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import kotlin.Error
import kotlin.random.Random

class AntaeusDal(private val db: Database) {
    fun fetchInvoice(id: Int): Invoice? {
        // transaction(db) runs the internal query as a new database transaction.
        return transaction(db) {
            // Returns the first invoice with matching id.
            InvoiceTable
                    .select { InvoiceTable.id.eq(id) }
                    .firstOrNull()
                    ?.toInvoice()
        }
    }

    //fetch invoices in batchSize start from offsetId

    fun fetchPendingInvoices(offsetId: Int, batchSize: Int, invoiceStatus: InvoiceStatus, timezone: String): List<Invoice> {
        if (!offsetId.equals(-1)) {
            return transaction(db) {
                InvoiceTable
                        .select {
                            InvoiceTable.id.greater<Int, Int>(offsetId) and
                                    InvoiceTable.timezone.eq(timezone.toString()) and
                                    InvoiceTable.status.eq(invoiceStatus.toString())
                        }
                        .limit(batchSize)
                        .sortedBy { InvoiceTable.id }
                        .map { it.toInvoice() }
            }
        } else {
            return transaction(db) {
                InvoiceTable
                        .select {
                            InvoiceTable.timezone.eq(timezone.toString()) and
                                    InvoiceTable.status.eq(invoiceStatus.toString())
                        }
                        .limit(batchSize)
                        .sortedBy { InvoiceTable.id }
                        .map { it.toInvoice() }
            }
        }
    }

    //update Invoice with Status
    fun updateInvoice(id: Int, statusTo: InvoiceStatus, idempotencyKey:String?=null,retryCount: Int = -1):Invoice? {
        transaction(db) {
            InvoiceTable.update({ InvoiceTable.id.eq(id) }) {
                it[InvoiceTable.status] = statusTo.toString()
                it[InvoiceTable.idempotencyKey] = idempotencyKey.toString()
                if (retryCount > 0) {
                    it[InvoiceTable.retryCount] = retryCount
                }
            }
        }
        val invoice = fetchInvoice(id)
        return invoice
    }

    fun updateInvoiceIdempotencyKey(customerId: Int,idempotencyKey: String?=null){
        InvoiceTable.update ( { InvoiceTable.customerId.eq(customerId) } ){
            if(null != idempotencyKey)
                it[InvoiceTable.idempotencyKey] = idempotencyKey.toString()
        }
    }

    fun fetchInvoices(): List<Invoice> {
        return transaction(db) {
            InvoiceTable
                    .selectAll()
                    .map { it.toInvoice() }
        }
    }

    fun createInvoice(amount: Money, customer: Customer, status: InvoiceStatus = InvoiceStatus.PENDING, timezone: String,idempotencyKey: String?=null, retryCount: Int = -1): Invoice? {
        val id = transaction(db) {
            // Insert the invoice and returns its new id.
            InvoiceTable
                    .insert {
                        it[this.value] = amount.value
                        it[this.currency] = amount.currency.toString()
                        it[this.status] = status.toString()
                        it[this.customerId] = customer.id
                        it[this.emailId] = String().format(customer.id, "@gmail.com").toString()
                        it[this.timezone] = timezone.toString()
                        it[this.retryCount] = retryCount
                        it[this.idempotencyKey]= idempotencyKey.toString()
                    } get InvoiceTable.id
        }

        return fetchInvoice(id)
    }

    fun fetchCustomer(id: Int): Customer? {
        return transaction(db) {
            CustomerTable
                    .select { CustomerTable.id.eq(id) }
                    .firstOrNull()
                    ?.toCustomer()
        }
    }

    fun fetchCustomers(): List<Customer> {
        return transaction(db) {
            CustomerTable
                    .selectAll()
                    .map { it.toCustomer() }
        }
    }

    fun createCustomer(currency: Currency): Customer? {
        val id = transaction(db) {
            // Insert the customer and return its new id.
            CustomerTable.insert {
                it[this.currency] = currency.toString()
            } get CustomerTable.id
        }

        return fetchCustomer(id)
    }

}
