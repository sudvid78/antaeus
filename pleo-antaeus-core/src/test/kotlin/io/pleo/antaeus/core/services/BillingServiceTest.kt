package io.pleo.antaeus.core.services

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.mockk.verifyOrder
import io.pleo.antaeus.core.external.PaymentProvider
import io.pleo.antaeus.data.AntaeusDal
import io.pleo.antaeus.models.Currency
import io.pleo.antaeus.models.Invoice
import io.pleo.antaeus.models.InvoiceStatus
import io.pleo.antaeus.models.Money
import org.joda.time.DateTime
import org.junit.jupiter.api.Test
import java.math.BigDecimal

class BillingServiceTest {
    private val invoiceList1 = ArrayList<Invoice>()
    private val invoiceList2 = ArrayList<Invoice>()
    private val invoiceList3 = ArrayList<Invoice>()

    private val invoiceListRetry1 = ArrayList<Invoice>()
    private val invoiceListRetry2 = ArrayList<Invoice>()

    /*
        timeZoneMap[Currency.DKK
                   Currency.GBP
                   Currency.EUR   =  "Europe/Copenhagen"
                  Currency.SEK]
    timeZoneMap[Currency.USD]    = "America/New_York"

    */
    init {
        invoiceList1.add(Invoice(id = 1, emailId = "1@gmail.com",status = InvoiceStatus.PENDING,
                amount = Money(BigDecimal.valueOf(10), Currency.DKK),
                customerId = 1, retryCount = 0, timezone = "Europe/Copenhagen"))
        invoiceList1.add(Invoice(id = 2, emailId = "2@gmail.com",status = InvoiceStatus.PENDING,
                amount = Money(BigDecimal.valueOf(100), Currency.DKK),
                customerId = 2, retryCount = 0, timezone = "Europe/Copenhagen"))

        invoiceList1.add(Invoice(id = 3, emailId = "3@gmail.com",status = InvoiceStatus.PENDING,
                amount = Money(BigDecimal.valueOf(10), Currency.DKK),
                customerId = 3, retryCount = 0, timezone = "Europe/Copenhagen"))

        invoiceList2.add(Invoice(id = 4, emailId = "4@gmail.com",status = InvoiceStatus.PENDING,
                amount = Money(BigDecimal.valueOf(100), Currency.DKK),
                customerId = 4, retryCount = 0, timezone = "Europe/Copenhagen"))

        invoiceList2.add(Invoice(id = 5, emailId = "5@gmail.com",status = InvoiceStatus.PENDING,
                amount = Money(BigDecimal.valueOf(90), Currency.DKK),
                customerId = 5, retryCount = 0, timezone = "Europe/Copenhagen"))

        invoiceList2.add(Invoice(id = 6, emailId = "6@gmail.com",status = InvoiceStatus.PENDING,
                amount = Money(BigDecimal.valueOf(90), Currency.DKK),
                customerId = 6, retryCount = 0, timezone = "Europe/Copenhagen"))

        invoiceList3.add(Invoice(id = 7, emailId = "7@gmail.com",status = InvoiceStatus.PENDING,
                amount = Money(BigDecimal.valueOf(20), Currency.DKK),
                customerId = 7, retryCount = 0, timezone = "Europe/Copenhagen"))

        invoiceList3.add(Invoice(id = 8, emailId = "8@gmail.com",status = InvoiceStatus.PENDING,
                amount = Money(BigDecimal.valueOf(20), Currency.DKK),
                customerId = 8, retryCount = 0, timezone = "Europe/Copenhagen"))

        invoiceList3.add(Invoice(id = 9, emailId = "9@gmail.com",status = InvoiceStatus.PENDING,
                amount = Money(BigDecimal.valueOf(20), Currency.DKK),
                customerId = 9, retryCount = 0, timezone = "Europe/Copenhagen"))

        invoiceListRetry1.add(Invoice(id = 1, status = InvoiceStatus.RETRY,
                amount = Money(BigDecimal.valueOf(10), Currency.DKK),
                customerId = 1, retryCount = 1,emailId = "1@gmail.com",timezone = "Europe/Copenhagen" ))
        invoiceListRetry1.add(Invoice(id = 2, status = InvoiceStatus.RETRY,
                amount = Money(BigDecimal.valueOf(100), Currency.DKK),
                customerId = 2, retryCount = 3, emailId = "2@gmail.com",timezone = "Europe/Copenhagen" ))
        invoiceListRetry1.add(Invoice(id = 7, status = InvoiceStatus.RETRY,
                amount = Money(BigDecimal.valueOf(100), Currency.DKK),
                customerId = 2, retryCount = 10, emailId = "2@gmail.com",timezone = "Europe/Copenhagen" ))

        invoiceListRetry2.add(Invoice(id = 9, status = InvoiceStatus.RETRY,
                amount = Money(BigDecimal.valueOf(10), Currency.DKK),
                customerId = 3, retryCount = 2, emailId = "3@gmail.com",timezone = "Europe/Copenhagen" ))

    }

    @Test
    fun `Test Pending Invoices`() {
        // Setup the dal mock
        val dal = mockk<AntaeusDal>(relaxed = true)  {

            every { fetchPendingInvoices(-1, 3, InvoiceStatus.PENDING,"Europe/Copenhagen") } returns invoiceList1
            every { fetchPendingInvoices(3, 3, InvoiceStatus.PENDING,"Europe/Copenhagen") } returns invoiceList2
            every { fetchPendingInvoices(6, 3, InvoiceStatus.PENDING,"Europe/Copenhagen") } returns invoiceList3
            every { fetchPendingInvoices(9, 3, InvoiceStatus.PENDING,"Europe/Copenhagen") } returns ArrayList<Invoice>()


            every { updateInvoice(id = 2, statusTo = InvoiceStatus.PAID,idempotencyKey=null ) } returns null
            every { updateInvoice(id = 1, statusTo = InvoiceStatus.RETRY) } returns
                    Invoice(id = 1, status = InvoiceStatus.RETRY, amount = Money(BigDecimal.valueOf(10), Currency.EUR),
                            customerId = 1, retryCount = 1,emailId = "1@gmail.com",timezone = "Europe/Brussels",idempotencyKey = "2we")

            every { updateInvoice(id = 3, statusTo = InvoiceStatus.RETRY) } returns
                    Invoice(id = 3, status = InvoiceStatus.RETRY,
                            amount = Money(BigDecimal.valueOf(10), Currency.DKK),
                            customerId = 3, retryCount = 1,emailId = "1@gmail.com",timezone = "Europe/Brussels",idempotencyKey = "2we")

            every { updateInvoice(id = 4, statusTo = InvoiceStatus.PAID) } returns
                    Invoice(id = 4, status = InvoiceStatus.PAID,
                            amount = Money(BigDecimal.valueOf(100), Currency.DKK),
                            customerId = 1, retryCount = 1,emailId = "1@gmail.com",timezone = "Europe/Brussels",idempotencyKey = "2we")
            every { updateInvoice(id = 5, statusTo = InvoiceStatus.RETRY) } returns
                    Invoice(id = 5, status = InvoiceStatus.RETRY,
                            amount = Money(BigDecimal.valueOf(10), Currency.DKK),
                            customerId = 3, retryCount = 1,emailId = "1@gmail.com",timezone = "Europe/Brussels",idempotencyKey = "2we")
            every { updateInvoice(id = 6, statusTo = InvoiceStatus.PAID) } returns
                    Invoice(id = 6, status = InvoiceStatus.PAID,
                            amount = Money(BigDecimal.valueOf(100), Currency.DKK),
                            customerId = 1, retryCount = 1,emailId = "1@gmail.com",timezone = "Europe/Brussels",idempotencyKey = "2we")
            every { updateInvoice(id = 7 , statusTo = InvoiceStatus.RETRY) } returns
                    Invoice(id = 7, status = InvoiceStatus.RETRY,
                            amount = Money(BigDecimal.valueOf(10), Currency.DKK),
                            customerId = 2, retryCount = 1,emailId = "1@gmail.com",timezone = "Europe/Brussels",idempotencyKey = "2we")
            every { updateInvoice(id = 8, statusTo = InvoiceStatus.PAID) } returns
                    Invoice(id = 8, status = InvoiceStatus.PAID,
                            amount = Money(BigDecimal.valueOf(100), Currency.DKK),
                            customerId = 2, retryCount = 1,emailId = "1@gmail.com",timezone = "Europe/Brussels",idempotencyKey = "2we")
            every { updateInvoice(id = 9, statusTo = InvoiceStatus.RETRY) } returns
                    Invoice(id = 9, status = InvoiceStatus.RETRY,
                            amount = Money(BigDecimal.valueOf(10), Currency.DKK),
                            customerId = 3, retryCount = 1,emailId = "1@gmail.com",timezone = "Europe/Brussels",idempotencyKey = "2we")
        }

        val invoiceService = InvoiceService(dal)
        val paymentProvider = object : PaymentProvider {
            override fun charge(invoice: Invoice): Boolean {
                return invoice.id % 2 == 0
            }
        }

        val billingService = BillingService(paymentProvider,invoiceService)

        billingService.processPendingInvoices(3,InvoiceStatus.PENDING,"Europe/Copenhagen");

        // Verify batch processing order
        verifyOrder {
            dal.fetchPendingInvoices(-1, 3,InvoiceStatus.PENDING,"Europe/Copenhagen") to invoiceList1
            dal.fetchPendingInvoices(3, 3,InvoiceStatus.PENDING,"Europe/Copenhagen") to invoiceList2
            dal.fetchPendingInvoices(6, 3,InvoiceStatus.PENDING,"Europe/Copenhagen") to invoiceList3
            dal.fetchPendingInvoices(9, 3,InvoiceStatus.PENDING,"Europe/Copenhagen") to ArrayList<Invoice>()
        }

        // Verify to fetch the batch 4 times, on the third try there should have been no more batches
        // to get for the retry frequency
       verify(atLeast = 4, atMost = 4) {
            dal.fetchPendingInvoices(any(),any(),any(),any())
        }

        // Verify 9 update attempts were made while processing 9 invoices
        verify(atLeast = 9, atMost = 9) {
            dal.updateInvoice(any(), any(), any(), any())
        }

         //Verify that incorrect invoice isn't fetched
        verify {
            dal.updateInvoice(id = 2, statusTo = InvoiceStatus.PAID,retryCount = any(), idempotencyKey = any()) to null
        }
    }

    @Test
    fun `Test Retry Invoices`() {
        val dal = mockk<AntaeusDal>(relaxed=true) {
            every { fetchPendingInvoices(-1, 3, InvoiceStatus.RETRY,"Europe/Copenhagen") } returns invoiceListRetry1
            every { fetchPendingInvoices(7, 3 ,InvoiceStatus.RETRY,"Europe/Copenhagen") } returns invoiceListRetry2
            every { fetchPendingInvoices(9, 3,InvoiceStatus.RETRY ,"Europe/Copenhagen")} returns ArrayList<Invoice>()

            every { updateInvoice(id = 1,  retryCount = 2, statusTo = InvoiceStatus.RETRY) } returns
                    Invoice(id = 1, status = InvoiceStatus.RETRY,
                            amount = Money(BigDecimal.valueOf(10), Currency.DKK),
                            customerId = 1, retryCount = 2,emailId = "1@gmail.com",timezone ="Europe/Copenhagen" )
            every { updateInvoice(id = 2, retryCount = -1, statusTo = InvoiceStatus.PAID) } returns
                    Invoice(id = 2, status = InvoiceStatus.PAID,
                            amount = Money(BigDecimal.valueOf(100), Currency.DKK),
                            customerId = 2, retryCount = -1, emailId = "2@gmail.com",timezone = "Europe/Copenhagen")
            every { updateInvoice(id = 7, retryCount  = 10, statusTo = InvoiceStatus.RETRY) } returns
                    Invoice(id = 7, status = InvoiceStatus.FAILED,
                            amount = Money(BigDecimal.valueOf(100), Currency.DKK),
                            customerId = 2, retryCount = 10,emailId = "7@gmail.com",timezone = "Europe/Copenhagen")
            every { updateInvoice(id = 9, retryCount = 3, statusTo = InvoiceStatus.RETRY) } returns
                    Invoice(id = 9, status = InvoiceStatus.RETRY,
                            amount = Money(BigDecimal.valueOf(10), Currency.DKK),
                            customerId = 3, retryCount = 3,emailId = "3@gmail.com",timezone = "Europe/Copenhagen")

        }

        val invoiceService = InvoiceService(dal)
        val paymentProvider = object : PaymentProvider {
            override fun charge(invoice: Invoice): Boolean {
                return invoice.id % 2 == 0
            }
        }

        val billingService = BillingService(paymentProvider, invoiceService)

       billingService.retryPendingInvoices(3,InvoiceStatus.RETRY,timezone = "Europe/Copenhagen")

        // Verify that the the offset was correctly being set everytime we fetched a new batch to process
        verifyOrder {
            dal.fetchPendingInvoices(-1, 3,InvoiceStatus.RETRY,timezone = "Europe/Copenhagen") to invoiceListRetry1
            dal.fetchPendingInvoices(7, 3, InvoiceStatus.RETRY,timezone = "Europe/Copenhagen") to invoiceListRetry2
            dal.fetchPendingInvoices(9, 3,InvoiceStatus.RETRY,timezone = "Europe/Copenhagen") to ArrayList<Invoice>()
        }

        // Verify that we only tried to fetch the batch 3 times, on the third try there should have been no more batches
        // to get for the retry frequency
        verify(atLeast = 3, atMost = 3) {
            dal.fetchPendingInvoices(any(), any(), any(),any())
        }

        // Verify that only 4 update attempts were made because we only tried to process 4 invoices in all the batches
        // that were received.
        verify(atLeast = 4, atMost = 4) {
            dal.updateInvoice(any(), any(), any(), any())
        }

        // Verify that the invoice that had exceeded the maximum number of retries before failure actually failed.
        verify(atLeast = 1, atMost = 1) {
            dal.updateInvoice(id = 7,  retryCount = 10, statusTo = InvoiceStatus.FAILED,idempotencyKey = null)
        }

        // Verify that a successful PAID invoice  was retried .
        verify {
            dal.updateInvoice(id = 2, retryCount = -1, statusTo = InvoiceStatus.PAID,idempotencyKey = "2") } to
                    Invoice(id = 2, status = InvoiceStatus.PAID,
                            amount = Money(BigDecimal.valueOf(100), Currency.DKK),
                            customerId = 2, retryCount = -1, emailId = "2@gmail.com",timezone = "Europe/Copenhagen",idempotencyKey = "2")
        }
    }

