package io.pleo.antaeus.core.services


import io.pleo.antaeus.core.exceptions.LowBalanceException
import io.pleo.antaeus.core.exceptions.InvoiceExceptionWhileCharging
import io.pleo.antaeus.core.exceptions.NetworkException
import io.pleo.antaeus.core.exceptions.RetryExceededException
import io.pleo.antaeus.core.external.PaymentProvider
import io.pleo.antaeus.models.Currency
import io.pleo.antaeus.models.Invoice
import io.pleo.antaeus.models.InvoiceStatus
import org.slf4j.Logger
import java.math.BigDecimal
import java.net.Inet4Address
import java.time.Instant
import java.util.*


class charge(
        private val invoiceService: InvoiceService,
        private val paymentProvider: PaymentProvider,
        private val invoice: Invoice,
        private val logger:Logger

) : Runnable {
    /*
    * Make sure the exactly-once semantics i.e guard with client Idempotency key
    * Create a unique idempotency key and set while Charging the customer for the first time to ensure the exactly-once semantics
    * */
    override fun run() {

        logger.info(Instant.now().toString() + " Attempting to charge invoice ID: " + invoice.id)
        //Abort if not enough balance
        if (invoice.amount.value < getAppconfig().balanceNeeded) {
            invoiceService.updateInvoice(invoice.customerId, InvoiceStatus.RETRY,retryCount = invoice.retryCount+1)
            logger.error(Instant.now().toString() + " Not enough balance to charge invoice ID: " + invoice.id)
            throw LowBalanceException(invoice.id, invoice.customerId)
        }
        //Charging invoice for the first time, So, create a unique Idempotency key to make sure Exacty Once semantics
        if (invoice.idempotencyKey.toString().isNullOrBlank() || invoice.idempotencyKey == null || invoice.idempotencyKey.toString().isNullOrEmpty()) {
            var isPaymetSuccess: Boolean = false
            var idempotencyKey: String? = null
            var invoiceUpdated: Invoice? = invoice

            try {
                idempotencyKey = generateIdempotencyKey(invoice.customerId)
                isPaymetSuccess = paymentProvider.charge(invoice)
                if (isPaymetSuccess) {
                    invoiceUpdated = invoiceService.updateInvoice(invoice.id, InvoiceStatus.PAID, idempotencyKey.toString(),retryCount = -1)
                }else{
                    if(invoice.retryCount >= getAppconfig().maxRetryCount){
                        invoiceService.updateInvoice(invoice.id, InvoiceStatus.FAILED, retryCount = getAppconfig().maxRetryCount);
                    }else{
                        invoiceService.updateInvoice(invoice.id, InvoiceStatus.RETRY, retryCount = invoice.retryCount+1);
                    }
                    throw InvoiceExceptionWhileCharging(invoice.id)
                }
            } catch (e: NetworkException) {
                logger.error(Instant.now().toString() + " Network Exception during the operation for invoiceid ", invoice.id)
                e.printStackTrace()
            } catch (e: Exception) {
                logger.error(Instant.now().toString() + " Exception during the operation for invoiceid ", invoice.id)
                logger.error(e.printStackTrace().toString())
                e.printStackTrace()
            } finally {
                //clear the Idempotency key
                if (invoiceUpdated != null) {
                    if (isPaymetSuccess && invoiceUpdated.status.equals(InvoiceStatus.PAID.toString()) && idempotencyKey.toString().isNotEmpty() ) {
                        invoiceService.updateInvoiceIdempotencyKey(invoice.id)
                    }
                }
            }
        } else {

            logger.error(Instant.now().toString() + " Invalid Attempt. Customer of invoice", invoice.id.toString(), " been already charged. Invalid Attempt. Reason=", invoice.idempotencyKey.toString())
        }
    }
}
    class reminder(
            private val invoiceService: InvoiceService,
            private val invoice: Invoice,
            private val invoiceStatusTo: InvoiceStatus?=null,
            private val smtpServer:PleoSMTPServer,
            private val logger: Logger
    ) : Runnable {
        override fun run() {
            //send reminder mail to customer
            logger.info(Instant.now().toString() + "reminder mail being sent to invoice" ,invoice.emailId )
            val response = smtpServer.sendEmail(invoice.emailId)
            logger.info(Instant.now().toString() + response )
            if (invoiceStatusTo != null) {
                invoiceService.updateInvoice(invoice.id, invoiceStatusTo, retryCount = invoice.retryCount)
            }
        }
    }
    class util(){
        //timezone list
        fun getTimeZoneList(): List<String> {
            return listOf("Europe/Copenhagen",
                           "America/New_York")
        }

    }


    fun generateIdempotencyKey(id: Int): String {
        return id.toString()
    }


data class getAppconfig(
        val batchSize:Int = 10,
        val processDay:Int =1,
        val retryUpto:Int = 14,
        val reminderDay:Int = 25,
        val maxRetryCount:Int=10,
        val balanceNeeded: BigDecimal = BigDecimal.valueOf(50.00)

)
//mock SMTP server
class PleoSMTPServer() {

    fun sendEmail(toAddr: String):String{
        val response = ("Successfully sent to " + toAddr.toString())
        return response
    }
}