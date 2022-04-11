package io.pleo.antaeus.services

import io.pleo.antaeus.events.ApplicationErrorEvent
import io.pleo.antaeus.events.BusinessErrorEvent
import io.pleo.antaeus.events.InvoiceStatusChangedEvent
import io.pleo.antaeus.external.EventNotificator
import io.pleo.antaeus.models.Invoice
import io.pleo.antaeus.models.InvoiceStatus

class InvoiceDomainService(private val eventNotificator: EventNotificator) {
    suspend fun pay(invoice: Invoice) {
        invoice.status = InvoiceStatus.PAID
        val event = InvoiceStatusChangedEvent(
            resourceId = invoice.id,
            resourceName = Invoice::class.simpleName!!,
            oldStatus = InvoiceStatus.PENDING.toString(),
            newStatus = InvoiceStatus.PAID.toString()
        )
        eventNotificator.notify(event)
    }

    suspend fun decline(invoice: Invoice) {
        val reason = "Invoice charge declined due lack of account balance of customer '${invoice.customerId}'"
        val event = BusinessErrorEvent(
            resourceId = invoice.id,
            resourceName = Invoice::class.simpleName!!,
            reason = reason
        )
        eventNotificator.notify(event)
    }

    suspend fun uncollect(invoice: Invoice, exception: Exception) {
        invoice.status = InvoiceStatus.UNCOLLECTIBLE
        val failureEvent = BusinessErrorEvent(
            resourceId = invoice.id,
            resourceName = Invoice::class.simpleName!!,
            exception = exception
        )
        val changeEvent = InvoiceStatusChangedEvent(
            resourceId = invoice.id,
            resourceName = Invoice::class.simpleName!!,
            oldStatus = InvoiceStatus.PENDING.toString(),
            newStatus = InvoiceStatus.UNCOLLECTIBLE.toString()
        )
        eventNotificator.notify(failureEvent)
        eventNotificator.notify(changeEvent)
    }

    suspend fun overdue(invoice: Invoice) {
        invoice.status = InvoiceStatus.OVERDUE
        val event = InvoiceStatusChangedEvent(
            resourceId = invoice.id,
            resourceName = Invoice::class.simpleName!!,
            oldStatus = InvoiceStatus.PENDING.toString(),
            newStatus = InvoiceStatus.OVERDUE.toString()
        )
        eventNotificator.notify(event)
    }

    suspend fun fail(id: Int, exception: Exception) {
        val event = ApplicationErrorEvent(
            resourceId = id,
            resourceName = Invoice::class.simpleName!!,
            exception = exception
        )
        eventNotificator.notify(event)
    }
}
