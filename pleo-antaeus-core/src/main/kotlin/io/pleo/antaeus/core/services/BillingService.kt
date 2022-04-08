package io.pleo.antaeus.core.services

import io.pleo.antaeus.core.events.BusinessErrorEvent
import io.pleo.antaeus.core.exceptions.CurrencyMismatchException
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
                when(exception) {
                    is CurrencyMismatchException -> {
                        val event = BusinessErrorEvent(invoice.id, "Invoice", exception = exception)
                        failureHandler.notify(event)
                    }
                }
            }
        }
    }
}
