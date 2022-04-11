
import io.pleo.antaeus.core.events.ApplicationErrorEvent
import io.pleo.antaeus.core.events.BusinessErrorEvent
import io.pleo.antaeus.core.events.Event
import io.pleo.antaeus.core.external.EventNotificator
import io.pleo.antaeus.core.external.PaymentProvider
import io.pleo.antaeus.data.AntaeusDal
import io.pleo.antaeus.models.Currency
import io.pleo.antaeus.models.Invoice
import io.pleo.antaeus.models.InvoiceStatus
import io.pleo.antaeus.models.Money
import kotlinx.coroutines.delay
import org.slf4j.LoggerFactory
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
                status = if (it == 1) InvoiceStatus.PENDING else InvoiceStatus.PAID
            )
        }
    }
}

// This is the mocked instance of the payment provider
internal fun getPaymentProvider(): PaymentProvider {
    return object : PaymentProvider {
       override suspend fun charge(invoice: Invoice): Boolean {
            delay(Random.nextLong(300, 2000))
            return Random.nextBoolean()
        }
    }
}

internal fun getEventNotificator(): EventNotificator {
    return object : EventNotificator {
        val logger = LoggerFactory.getLogger("EventNotificator")

        override suspend fun notify(event: Event) {
            when(event) {
                is BusinessErrorEvent -> logger.info("The following failure occurred: ${event.reason}")
                is ApplicationErrorEvent -> logger.info("The following failure occurred: ${event.exception.printStackTrace()}")
            }
        }
    }
}
