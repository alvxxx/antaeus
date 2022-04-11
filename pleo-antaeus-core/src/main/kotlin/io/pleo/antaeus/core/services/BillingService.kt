package io.pleo.antaeus.core.services

import io.pleo.antaeus.core.events.ApplicationErrorEvent
import io.pleo.antaeus.core.events.BusinessErrorEvent
import io.pleo.antaeus.core.events.Event
import io.pleo.antaeus.core.events.InvoiceStatusChangedEvent
import io.pleo.antaeus.core.exceptions.CurrencyMismatchException
import io.pleo.antaeus.core.exceptions.CustomerNotFoundException
import io.pleo.antaeus.core.exceptions.NetworkException
import io.pleo.antaeus.core.external.EventNotificator
import io.pleo.antaeus.core.external.PaymentProvider
import io.pleo.antaeus.data.AntaeusDal
import io.pleo.antaeus.models.Invoice
import io.pleo.antaeus.models.InvoiceStatus
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import java.util.concurrent.atomic.AtomicInteger

class BillingService(
    private val paymentProvider: PaymentProvider,
    private val dal: AntaeusDal,
    private val eventNotificator: EventNotificator,
    private val numberOfCoroutines: Int = 16
) {
    private val logger = LoggerFactory.getLogger(javaClass.simpleName)

    fun chargeInvoices() = runBlocking {
        initPendingInvoiceIterator {
            val events = try { charge(it) } catch (ex: Exception) { handleFailure(ex, it) }
            events.forEach { ev -> eventNotificator.notify(ev) }
            dal.updateInvoice(it)
        }
    }

    fun overdueInvoices() = runBlocking {
        initPendingInvoiceIterator {
            it.overdue()
            val eventChange = InvoiceStatusChangedEvent(it.id, it.javaClass.simpleName, InvoiceStatus.PENDING.toString(), InvoiceStatus.OVERDUE.toString())
            eventNotificator.notify(eventChange)
            dal.updateInvoice(it)
        }
    }

    private suspend fun initPendingInvoiceIterator(action: suspend (Invoice) -> Unit) = runBlocking {
        val currentPage = AtomicInteger(-numberOfCoroutines)
        repeat(numberOfCoroutines) {
            launch {
                do {
                    val nextPage = currentPage.addAndGet(numberOfCoroutines)
                    logger.info("The next page to be fetch is: ${nextPage / numberOfCoroutines}")
                    val invoicesToCharge = dal.fetchInvoicePageByStatus(InvoiceStatus.PENDING, numberOfCoroutines, nextPage)
                    invoicesToCharge.forEach { invoice -> action.invoke(invoice) }
                } while (invoicesToCharge.isNotEmpty())
            }
        }
    }

    private suspend fun charge(invoice: Invoice): List<Event> {
        val events: MutableList<Event> = mutableListOf()
        val wasCharged = paymentProvider.charge(invoice)
        if (wasCharged) {
            invoice.pay()
            val eventChange = InvoiceStatusChangedEvent(invoice.id, invoice.javaClass.simpleName, InvoiceStatus.PENDING.toString(), InvoiceStatus.PAID.toString())
            events.add(eventChange)
        } else {
            val reason = "Invoice charge declined due lack of account balance of customer '${invoice.customerId}'"
            val eventFailure = BusinessErrorEvent(invoice.id, invoice.javaClass.simpleName, reason)
            events.add(eventFailure)
        }
        return events
    }

    private fun handleFailure(exception: Exception, invoice: Invoice): List<Event> {
        val events: MutableList<Event> = mutableListOf()
        when (exception) {
            is CurrencyMismatchException,
            is CustomerNotFoundException -> {
                invoice.uncollect()
                val failureEvent = BusinessErrorEvent(invoice.id, invoice.javaClass.simpleName, exception = exception)
                val eventChange = InvoiceStatusChangedEvent(invoice.id, invoice.javaClass.simpleName, InvoiceStatus.PENDING.toString(), InvoiceStatus.UNCOLLECTIBLE.toString())
                events.add(failureEvent)
                events.add(eventChange)
            }
            is NetworkException -> {
                val failureEvent = ApplicationErrorEvent(invoice.id, invoice.javaClass.simpleName, exception = exception)
                events.add(failureEvent)
            }
        }
        return events
    }
}
