package io.pleo.antaeus.core.services

import io.pleo.antaeus.core.external.PaymentProvider
import io.pleo.antaeus.models.Currency
import io.pleo.antaeus.models.Invoice
import io.pleo.antaeus.models.InvoiceStatus
import org.slf4j.LoggerFactory
import java.util.Timer
import kotlin.concurrent.fixedRateTimer
import java.time.*
import java.time.ZoneId
import java.util.*
import kotlin.collections.HashMap



class BillingService(
    private val paymentProvider: PaymentProvider,
    private val invoiceService: InvoiceService
) {
    private val logger = LoggerFactory.getLogger("BillingService")
    private val timerList=LinkedList<Any>()

    //util function for mapOf<Currency,timezone>
    fun getTimeZoneforCurrency(): HashMap<Currency, String> {
        val timeZoneMap = hashMapOf<Currency,String>()
        timeZoneMap[Currency.DKK]="Europe/Copenhagen"
        timeZoneMap[Currency.USD]="America/New_York"
        timeZoneMap[Currency.EUR]="Europe/Brussels"
        timeZoneMap[Currency.GBP]="Europe/London"
        timeZoneMap[Currency.SEK]="Europe/Copenhagen"
        return timeZoneMap
    }

    /*
    *   start the timer for different timezones which runs for every 24 hours
    *   On DAY==1, charge pending invoices
    *       change Invoice status = PAID upon successful charging
    *       change Invoice status = RETRY upon failure
    *   On DAY==25, send reminder mails to customers
    *       change the PAID Invoice status to Pending
    *   For all days, run the retry jobs
    *
    *   As of now, all process are executed in sequential manner i.e. for loop to process invoices
    *   Later this will be changed to adapt concurrency or Parallelism with Reactive Programming or
    *   batch processing with rate limiter
    *
    *   Can be changed to java scheduled executor service instead of Timer
    *
    * */
    fun startInvoiceProcessingTimerTimeZones(){
        //check and start scheduler per timezone
        getTimeZoneforCurrency().values.forEach {
                timerList.add(fixedRateTimer(it+"-Scheduler",startAt = Date(),
                                period = 24 * 60 * 60 * 1000){
                                    val index=0
                                    val z = ZoneId.of(it)
                                    val ld: LocalDate = LocalDate.now(z)
                                    val dayOfMonth: Int = ld.getDayOfMonth()
                                    //start charging the Pending Invoices
                                    println(it)
                                    println(dayOfMonth)
                                    if (dayOfMonth == 1) {
                                        chargePendingInvoices(it)//timezone
                                        //PAID
                                    }else if (dayOfMonth == 25){ //send reminder mails to customers when DAY=25
                                        sendReminderForCharging(it)
                                    }else {
                                        retryFailedJobs(it)
                                    }
                                })
         }
    }

    //start charging the Pending Invoices on DAY=1 according to the TimeZone
    fun chargePendingInvoices(timezone: String){
        val invoiceList = invoiceService.fetchPendingInvoices(timezone)
        for (item in invoiceList) {
            if(paymentProvider.charge(item)){
                invoiceService.updateInvoiceStatus(item.customerId,InvoiceStatus.PAID.toString())
            }
            else{
                invoiceService.updateInvoiceStatus(item.customerId,InvoiceStatus.RETRY.toString())
            }
        }
    }

    //send reminder mails for Pending Invoices on DAY=25 according to the TimeZone
    fun sendReminderForCharging(timezone: String){
        val invoiceList = invoiceService.fetchPendingInvoices(timezone)
        for (item in invoiceList) {
            //send reminder mail to customer
            invoiceService.updateInvoiceStatus(item.customerId,InvoiceStatus.PENDING.toString())
        }
    }

    //retry failed jobs for FailedInvoices and update status accordingly
    fun retryFailedJobs(timezone: String){
        val invoiceList=invoiceService.fetchPendingInvoices(timezone)
        for (item in invoiceList) {
            if(paymentProvider.charge(item)){
//                invoiceService.updateInvoiceStatus(item.customerId,InvoiceStatus.PAID.toString())
            }
        }
    }


}
