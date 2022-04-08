package io.pleo.antaeus.core.services

import io.pleo.antaeus.core.events.ApplicationErrorEvent
import io.pleo.antaeus.core.events.BusinessErrorEvent
import io.pleo.antaeus.core.events.FailureEvent
import io.pleo.antaeus.core.exceptions.CurrencyMismatchException
import io.pleo.antaeus.core.exceptions.CustomerNotFoundException
import io.pleo.antaeus.core.exceptions.NetworkException
import io.pleo.antaeus.core.external.FailureHandler
import io.pleo.antaeus.core.external.PaymentProvider
import io.pleo.antaeus.data.AntaeusDal
import io.pleo.antaeus.models.InvoiceStatus

class BillingService(
    private val paymentProvider: PaymentProvider,
    private val dal: AntaeusDal,
    private val failureHandler: FailureHandler
) {
    fun handle() {
        val invoicesToCharge = dal.fetchInvoicesByStatus(InvoiceStatus.PENDING)
        for (invoice in invoicesToCharge) {
            try {
                val result = paymentProvider.charge(invoice)
                if (result) {
                    invoice.pay()
                    dal.updateInvoice(invoice)
                }
            } catch (exception: Exception) {
                var event: FailureEvent? = null
                when(exception) {
                    is CurrencyMismatchException,
                    is CustomerNotFoundException
                        -> event = BusinessErrorEvent(invoice.id, invoice.javaClass.simpleName, exception = exception)

                    is NetworkException
                        ->  event = ApplicationErrorEvent(invoice.id, invoice.javaClass.simpleName, exception = exception)
                }
                if (event != null) {
                    failureHandler.notify(event)
                }
            }
        }
    }
}
