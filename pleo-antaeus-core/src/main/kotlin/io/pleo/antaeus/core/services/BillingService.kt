package io.pleo.antaeus.core.services

import io.pleo.antaeus.core.exceptions.CurrencyMismatchException
import io.pleo.antaeus.core.exceptions.CustomerNotFoundException
import io.pleo.antaeus.core.exceptions.InvoiceNotFoundException
import io.pleo.antaeus.core.exceptions.NetworkException
import io.pleo.antaeus.core.external.PaymentProvider
import io.pleo.antaeus.data.AntaeusDal
import io.pleo.antaeus.models.Invoice
import io.pleo.antaeus.services.InvoiceDomainService
import kotlinx.coroutines.coroutineScope

class BillingService(
    private val paymentProvider: PaymentProvider,
    private val dal: AntaeusDal,
    private val domainService: InvoiceDomainService
) {
    suspend fun chargeInvoices() = coroutineScope {
        dal.onEveryPendingInvoice { charge(it) }
    }

    suspend fun overdueInvoices() = coroutineScope {
        dal.onEveryPendingInvoice { overdue(it) }
    }

    suspend fun chargeInvoiceById(id: Int) = coroutineScope {
        when(val it =  dal.fetchInvoice(id)?: InvoiceNotFoundException(id)) {
            is Invoice -> charge(it)
            is InvoiceNotFoundException -> domainService.fail(id, it)
        }
    }

    internal suspend fun overdue(it: Invoice) {
        domainService.overdue(it)
        dal.updateInvoice(it)
    }

    internal suspend fun charge(it: Invoice) {
        try {
            val wasCharged = paymentProvider.charge(it)
            if (wasCharged) domainService.pay(it) else domainService.decline(it)
        } catch (ex: Exception) {
            when (ex) {
                is CurrencyMismatchException,
                is CustomerNotFoundException -> domainService.uncollect(it, ex)
                is NetworkException -> domainService.fail(it.id, ex)
            }
        }
        dal.updateInvoice(it)
    }
}
