package io.pleo.antaeus.core.services

import io.pleo.antaeus.core.external.PaymentProvider
import io.pleo.antaeus.data.AntaeusDal
import io.pleo.antaeus.models.InvoiceStatus

class BillingService(
    private val paymentProvider: PaymentProvider,
    private val dal: AntaeusDal
) {
    fun handle() {
        val invoicesToCharge = dal.fetchInvoicesByStatus(InvoiceStatus.PENDING)
        for (invoice in invoicesToCharge) {
            val result = paymentProvider.charge(invoice)
            if (result){
                invoice.pay()
                dal.updateInvoice(invoice)
            }
        }
    }
}
