
import io.pleo.antaeus.core.events.ApplicationErrorEvent
import io.pleo.antaeus.core.events.BusinessErrorEvent
import io.pleo.antaeus.core.events.FailureEvent
import io.pleo.antaeus.core.external.FailureNotificator
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
            delay(Random.nextLong(300, 3000))
            return Random.nextBoolean()
        }
    }
}

internal fun getFailureNotificator(): FailureNotificator {
    return object : FailureNotificator {
        val logger = LoggerFactory.getLogger("FailureNotificator")

        override fun notify(failure: FailureEvent) {
            when(failure) {
                is BusinessErrorEvent -> logger.info("The following failure occurred: ${failure.reason}")
                is ApplicationErrorEvent -> logger.info("The following failure occurred: ${failure.exception.printStackTrace()}")
            }
        }
    }
}
