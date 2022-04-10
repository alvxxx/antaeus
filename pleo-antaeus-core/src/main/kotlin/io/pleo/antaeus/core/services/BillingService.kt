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

class BillingService(
    private val paymentProvider: PaymentProvider,
    private val dal: AntaeusDal,
    private val failureNotificator: FailureNotificator,
    private val numberOfCoroutines: Int = 16
) {
    suspend fun handle() {
        var page = 0
        do {
            val invoicesToCharge = dal.fetchInvoicePageByStatus(InvoiceStatus.PENDING, numberOfCoroutines, page)
            invoicesToCharge.forEach {
                val failureEvent = try { chargeInvoice(it) } catch (ex: Exception) { getFailureEvent(ex, it) }
                if (failureEvent != null) {
                    failureNotificator.notify(failureEvent)
                }
            }
            page += numberOfCoroutines
        } while (invoicesToCharge.isNotEmpty())
    }

    private suspend fun chargeInvoice(invoice: Invoice): FailureEvent? {
        var event: FailureEvent? = null
        val wasCharged = paymentProvider.charge(invoice)
        if (wasCharged) {
            invoice.pay()
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
