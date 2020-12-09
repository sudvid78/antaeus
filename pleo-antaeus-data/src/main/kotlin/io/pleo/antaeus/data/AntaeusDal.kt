/*
    Implements the data access layer (DAL).
    The data access layer generates and executes requests to the database.

    See the `mappings` module for the conversions between database rows and Kotlin objects.
 */

package io.pleo.antaeus.data

import io.pleo.antaeus.models.Currency
import io.pleo.antaeus.models.Customer
import io.pleo.antaeus.models.Invoice
import io.pleo.antaeus.models.InvoiceStatus
import io.pleo.antaeus.models.Money
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction

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

    //fetch the Invoices based on TimeZone
    fun fetchPendingInvoices(timezone: String): List<Invoice> {
        return transaction(db) {
            InvoiceTable
                    .select { InvoiceTable.timezone.eq(timezone.toString()) }
                    .map { it.toInvoice() }
        }
    }
    //update Invoice with Status
    fun updateInvoicesStatus(customerId:Int,statusTo:String){
        InvoiceTable.update ({InvoiceTable.customerId.eq(customerId)}){
                 it[InvoiceTable.status]=statusTo
        }
    }



    /*fun createTimeZone(){
        timeZoneMap[Currency.DKK]="Europe/Copenhagen"
        timeZoneMap[Currency.USD]="America/New_York"
        timeZoneMap[Currency.EUR]="Europe/Brussels"
        timeZoneMap[Currency.GBP]="Europe/London"
        timeZoneMap[Currency.SEK]="Europe/Copenhagen"
        TimeZoneTable.insert {
            it[this.currency] = currency.toString()
        } get CustomerTable.id
        val timeZoneMap = hashMapOf<Currency,String>()

    }*/

    /*fun fetchInvoicesTimezone(timezone: String): List<Invoice>
    //fetch Invoice with status=Pending
    //fun fetchPendingInvoices(): List<Invoice> {
        // transaction(db) runs the internal query as a new database transaction.
        return transaction(db) {
            // Returns the first invoice with matching id.
            InvoiceTable
                    .select{InvoiceTable.status.eq((InvoiceStatus.PENDING).toString())}
                    .orderBy(InvoiceTable.id, true)
                    .map { it.toInvoice() }
        }
    }
*/
    fun fetchInvoices(): List<Invoice> {
        return transaction(db) {
            InvoiceTable
                .selectAll()
                .map { it.toInvoice() }
        }
    }

    fun createInvoice(amount: Money, customer: Customer, status: InvoiceStatus = InvoiceStatus.PENDING, timezone: String): Invoice? {
        val id = transaction(db) {
            // Insert the invoice and returns its new id.
            InvoiceTable
                .insert {
                    it[this.value] = amount.value
                    it[this.currency] = amount.currency.toString()
                    it[this.status] = status.toString()
                    it[this.customerId] = customer.id
                    it[this.timezone] = timezone.toString()
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
