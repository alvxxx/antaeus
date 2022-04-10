package io.pleo.antaeus.core.services

import io.pleo.antaeus.core.events.ApplicationErrorEvent
import io.pleo.antaeus.core.events.BusinessErrorEvent
import io.pleo.antaeus.core.events.FailureEvent
import io.pleo.antaeus.core.exceptions.CurrencyMismatchException
import io.pleo.antaeus.core.exceptions.CustomerNotFoundException
import io.pleo.antaeus.core.exceptions.NetworkException
import io.pleo.antaeus.core.external.FailureNotificator
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
    private val failureNotificator: FailureNotificator,
    private val numberOfCoroutines: Int = 16
) {
    private val currentPage = AtomicInteger(-numberOfCoroutines)
    private val logger = LoggerFactory.getLogger(javaClass.simpleName)

    fun handle() = runBlocking {
        repeat(numberOfCoroutines) {
            launch { initProcess() }
        }
    }

    private suspend fun initProcess() {
        do {
            val nextPage = currentPage.addAndGet(numberOfCoroutines)
            logger.info("The next page to be fetch is: $nextPage")
            val invoicesToCharge = dal.fetchInvoicePageByStatus(InvoiceStatus.PENDING, numberOfCoroutines, nextPage)
            invoicesToCharge.forEach { invoice ->
                val failureEvent = try { charge(invoice) } catch (ex: Exception) { getFailureEvent(ex, invoice) }
                failureEvent?.let { failureNotificator.notify(it) }
            }
        } while (invoicesToCharge.isNotEmpty())
    }

    private suspend fun charge(invoice: Invoice): FailureEvent? {
        var event: FailureEvent? = null
        val wasCharged = paymentProvider.charge(invoice)
        if (wasCharged) {
            invoice.pay()
            logger.info("Invoice '${invoice.id}' of customer '${invoice.customerId}' was charged successfully")
            dal.updateInvoice(invoice)
        } else {
            val reason = "Invoice charge declined due lack of account balance of customer '${invoice.customerId}'"
            event = BusinessErrorEvent(invoice.id, invoice.javaClass.simpleName, reason)
        }
        return event
    }

    private fun getFailureEvent(exception: Exception, invoice: Invoice): FailureEvent? {
        var event: FailureEvent? = null
        when (exception) {
            is CurrencyMismatchException,
            is CustomerNotFoundException
                -> event = BusinessErrorEvent(invoice.id, invoice.javaClass.simpleName, exception = exception)
            is NetworkException
                -> event = ApplicationErrorEvent(invoice.id, invoice.javaClass.simpleName, exception = exception)
        }
        return event
    }
}
