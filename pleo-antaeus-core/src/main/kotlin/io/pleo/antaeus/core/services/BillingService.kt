package io.pleo.antaeus.core.services

import io.pleo.antaeus.core.external.PaymentProvider
import io.pleo.antaeus.models.Invoice
import io.pleo.antaeus.models.InvoiceStatus
import org.slf4j.LoggerFactory
import java.time.LocalDate
import java.time.ZoneId
import java.util.*
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.time.Instant

class BillingService(
    private val paymentProvider: PaymentProvider,
    private val invoiceService: InvoiceService
    // Initialize appConfig with batchSize, DAY_OF_MONTH to process invoice and send reminder

) {

    private val logger = LoggerFactory.getLogger("BillingService")
    private val timerList=LinkedList<Any>()
    private val corePoolSize = 100

    //Ideally corePoolSize=100 is decided based on benchmarking with load test during test phase
    private val executorAsync = Executors.newFixedThreadPool(corePoolSize)
    private val util=io.pleo.antaeus.core.services.util()
    private val executorScheduler = Executors.newScheduledThreadPool(util.getTimeZoneList().size)

    /*
    *   start the timer for different timezones which runs for every 24 hours
    *   On DAY==1, charge pending invoices
    *       change Invoice status = PAID upon successful charging
    *       change Invoice status = RETRY upon failure
    *   On DAY==25, send reminder mails to customers
    *       change the PAID Invoice status to Pending
    *   For all remaining days , run the retry jobs. Now this could be modified with anything per Business rules
    *
    * */
    fun triggerPendingInvoicesScheduler(){
        //check and start scheduler per timezone

        logger.info(Instant.now().toString()+" Starting Scheduler for different timezones...")
        util.getTimeZoneList().forEach {
            //start scheduler for different timezones
            val worker= {
                val z = ZoneId.of(it)
                val ld: LocalDate = LocalDate.now(z)
                val dayOfMonth: Int = ld.getDayOfMonth()

                //dayOfMonth=1 , start charging the Pending Invoices
                if (dayOfMonth == getAppconfig().processDay) {
                    processPendingInvoices(getAppconfig().batchSize,InvoiceStatus.PENDING, it)//timezone
                }else if (dayOfMonth == getAppconfig().reminderDay){ //dayOfMonth=25, send reminder mails
                    sendReminderAsync(getAppconfig().batchSize,InvoiceStatus.PAID,InvoiceStatus.PENDING,it)
                }else if(dayOfMonth <= getAppconfig().retryUpto){ // until dayofMonth=14, retry processing failed payments
                    sendReminderAsync(getAppconfig().batchSize,InvoiceStatus.RETRY,null,it)
                    retryPendingInvoices(getAppconfig().batchSize,InvoiceStatus.RETRY, it)//timezone
                }
            }
            //runs the scheduler once for every 24 hours
            executorScheduler.scheduleWithFixedDelay(worker,0,24 * 60 * 60 * 1000,TimeUnit.MILLISECONDS)
            logger.info(Instant.now().toString()+" Starting scheduler for timezone: "+it.toString());
            timerList.add(executorScheduler)
        }

    }
    /*
        Start processing pending Invoices
    */
    fun processPendingInvoices(batchSize:Int,invoiceStatus:InvoiceStatus , timezone: String) {
        logger.info(Instant.now().toString()+" Start processing pending invoices")
        var invoices = invoiceService.fetchPendingInvoices(-1, batchSize,invoiceStatus, timezone)
        while (null != invoices && invoices.size > 0) {
            //init the charge class and process the invoice async
            processInvoiceAsync(invoices)
            val lastOffSetId = invoices.get(invoices.size-1).id
            invoices=invoiceService.fetchPendingInvoices(lastOffSetId,batchSize,invoiceStatus,timezone)
        }
    }

    /*
        Retry processing oending invoices
    */
    fun retryPendingInvoices(batchSize:Int,invoiceStatus:InvoiceStatus , timezone: String) {
        logger.info(Instant.now().toString()+" Retry processing pending invoices")
        var invoices = invoiceService.fetchPendingInvoices(-1, batchSize,invoiceStatus, timezone)
        while (null != invoices && invoices.size > 0) {
            //init the charge class and process the invoice async
            processInvoiceAsync(invoices)
            val lastOffSetId = invoices.get(invoices.size-1).id
            invoices=invoiceService.fetchPendingInvoices(lastOffSetId,batchSize,invoiceStatus,timezone)
        }
    }

    /*
        All processing Runnable tasks are submitted to ThreadPool Executor for async processing
        refer class charge for the payment steps
    */
    fun processInvoiceAsync(invoices:List<Invoice> ){
        for(invoice:Invoice in invoices){
            logger.info(Instant.now().toString()+" Submitting processing task for InvoiceId ",invoice.id);
            executorAsync.submit(charge(invoiceService,paymentProvider,invoice,logger))
        }
    }
    //send reminder mails for Pending Invoices upto 2 weeks
    fun sendReminderAsync(batchSize:Int,invoiceStatus:InvoiceStatus ,invoiceStatusTo:InvoiceStatus?=null, timezone: String) {
        var invoices = invoiceService.fetchPendingInvoices(-1, batchSize,invoiceStatus, timezone)
        while (null != invoices && invoices.size > 0) {
            //init the charge class and process the invoice async
            reminderInvoiceAsync(invoices,invoiceStatusTo)
            val lastOffSetId = invoices.get(invoices.size-1).id
            invoices=invoiceService.fetchPendingInvoices(lastOffSetId,batchSize,invoiceStatus,timezone)
        }
    }

    fun reminderInvoiceAsync(invoices:List<Invoice>,invoiceStatusTo:InvoiceStatus?=null){
        for(invoice:Invoice in invoices){
            logger.info(Instant.now().toString()+" Sending reminder for processing task for InvoiceId ",invoice.id);
            executorAsync.submit(reminder(invoiceService,invoice,invoiceStatusTo,PleoSMTPServer(),logger))
        }
    }
}
