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
        invoicesToCharge.forEach {
            paymentProvider.charge(it)
        }
    }
}
