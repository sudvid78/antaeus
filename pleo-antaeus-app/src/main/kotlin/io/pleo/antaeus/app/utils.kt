
import io.pleo.antaeus.core.external.PaymentProvider
import io.pleo.antaeus.data.AntaeusDal
import io.pleo.antaeus.models.Currency
import io.pleo.antaeus.models.Invoice
import io.pleo.antaeus.models.InvoiceStatus
import io.pleo.antaeus.models.Money
import java.math.BigDecimal
import kotlin.random.Random

// This will create all schemas and setup initial data
internal fun setupInitialData(dal: AntaeusDal) {

    val customers = (1..100).mapNotNull {
        dal.createCustomer(
            currency = Currency.values()[Random.nextInt(0, Currency.values().size)]
        )
    }
     customers.forEach { customer ->
        (1..10).forEach {
            dal.createInvoice(
                amount = Money(
                    value = BigDecimal(Random.nextDouble(10.0, 500.0)),
                    currency = customer.currency
                ),
                customer = customer,
                status = if (it == 1) InvoiceStatus.PENDING else InvoiceStatus.PAID,
                timezone = getTimeZoneforCurrency(customer.currency)


            )
        }
    }
}

// This is the mocked instance of the payment provider
internal fun getPaymentProvider(): PaymentProvider {
    return object : PaymentProvider {
        override fun charge(invoice: Invoice): Boolean {
                return Random.nextBoolean()
        }
    }
}

internal fun getTimeZoneforCurrency(currency: Currency): String {

   val timeZoneMap = hashMapOf<Currency,String>()
    timeZoneMap[Currency.DKK]="Europe/Copenhagen"
    timeZoneMap[Currency.USD]="America/New_York"
    timeZoneMap[Currency.EUR]="Europe/Copenhagen"
    timeZoneMap[Currency.GBP]="Europe/Copenhagen"
    timeZoneMap[Currency.SEK]="Europe/Copenhagen"
    return timeZoneMap.get(currency).toString()
}
