package io.pleo.antaeus.services

import io.mockk.coVerify
import io.mockk.mockk
import io.pleo.antaeus.events.ApplicationErrorEvent
import io.pleo.antaeus.events.BusinessErrorEvent
import io.pleo.antaeus.events.InvoiceStatusChangedEvent
import io.pleo.antaeus.external.EventNotificator
import io.pleo.antaeus.models.Currency
import io.pleo.antaeus.models.Invoice
import io.pleo.antaeus.models.InvoiceStatus
import io.pleo.antaeus.models.Money
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.lang.Exception
import java.math.BigDecimal

@OptIn(ExperimentalCoroutinesApi::class)
class InvoiceDomainServiceTest {
    private fun mockInvoice() = Invoice(1, 1, Money(BigDecimal(100), Currency.USD), InvoiceStatus.PENDING)
    private val eventNotificator = mockk<EventNotificator>(relaxed = true)
    private val sut = InvoiceDomainService(eventNotificator)

    @Test
    fun `will pay invoice`() = runTest {
        val invoice = mockInvoice()

        sut.pay(invoice)

        assert(invoice.status == InvoiceStatus.PAID)
    }

    @Test
    fun `will overdue invoice`() = runTest {
        val invoice = mockInvoice()

        sut.overdue(invoice)

        assert(invoice.status == InvoiceStatus.OVERDUE)
    }

    @Test
    fun `will uncollect invoice`() = runTest {
        val invoice = mockInvoice()
        val exception = Exception("any_message")

        sut.uncollect(invoice, exception)

        assert(invoice.status == InvoiceStatus.UNCOLLECTIBLE)
    }

    @Test
    fun `will notify paid invoice changes`() = runTest {
        val invoice = mockInvoice()

        sut.pay(invoice)

        coVerify(exactly = 1) { eventNotificator.notify(withArg <InvoiceStatusChangedEvent> {
            Assertions.assertTrue(it.resourceName == "Invoice")
            Assertions.assertTrue(it.resourceId == 1)
            Assertions.assertTrue(it.oldStatus == "PENDING")
            Assertions.assertTrue(it.newStatus == "PAID")
        }) }
    }

    @Test
    fun `will notify overdue invoice changes`() = runTest {
        val invoice = mockInvoice()

        sut.overdue(invoice)

        coVerify(exactly = 1) { eventNotificator.notify(withArg <InvoiceStatusChangedEvent> {
            Assertions.assertTrue(it.resourceName == "Invoice")
            Assertions.assertTrue(it.resourceId == 1)
            Assertions.assertTrue(it.oldStatus == "PENDING")
            Assertions.assertTrue(it.newStatus == "OVERDUE")
        }) }
    }

    @Test
    fun `will notify uncollect invoice changes`() = runTest {
        val invoice = mockInvoice()
        val exception = Exception("any_message")

        sut.uncollect(invoice, exception)

        coVerify(exactly = 1) { eventNotificator.notify(withArg <BusinessErrorEvent> {
            Assertions.assertTrue(it.resourceName == "Invoice")
            Assertions.assertTrue(it.resourceId == 1)
            Assertions.assertTrue(it.reason == null)
            Assertions.assertTrue(it.exception?.message == "any_message")
        }) }
    }

    @Test
    fun `will notify failed invoice charges`() = runTest {
        val exception = Exception("any_message")

        sut.fail(1, exception)

        coVerify(exactly = 1) { eventNotificator.notify(withArg <ApplicationErrorEvent> {
            Assertions.assertTrue(it.resourceName == "Invoice")
            Assertions.assertTrue(it.resourceId == 1)
            Assertions.assertTrue(it.exception.message == "any_message")
        }) }
    }

    @Test
    fun `will notify declined invoice charges`() = runTest {
        val invoice = mockInvoice()

        sut.decline(invoice)

        coVerify(exactly = 1) { eventNotificator.notify(withArg <BusinessErrorEvent> {
            Assertions.assertTrue(it.resourceName == "Invoice")
            Assertions.assertTrue(it.resourceId == 1)
            Assertions.assertTrue(it.reason == "Invoice charge declined due lack of account balance of customer '1'")
        }) }
    }
}