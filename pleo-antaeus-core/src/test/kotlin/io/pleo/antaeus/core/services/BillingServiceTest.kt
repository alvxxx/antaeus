package io.pleo.antaeus.core.services

import io.mockk.*
import io.pleo.antaeus.core.events.ApplicationErrorEvent
import io.pleo.antaeus.core.events.BusinessErrorEvent
import io.pleo.antaeus.core.exceptions.CurrencyMismatchException
import io.pleo.antaeus.core.exceptions.CustomerNotFoundException
import io.pleo.antaeus.core.exceptions.NetworkException
import io.pleo.antaeus.core.external.FailureNotificator
import io.pleo.antaeus.core.external.PaymentProvider
import io.pleo.antaeus.data.AntaeusDal
import io.pleo.antaeus.models.Currency
import io.pleo.antaeus.models.Invoice
import io.pleo.antaeus.models.InvoiceStatus
import io.pleo.antaeus.models.Money
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.math.BigDecimal

class BillingServiceTest {
    private val dal = mockk<AntaeusDal>(relaxed = true)
    private val paymentProvider = mockk<PaymentProvider>(relaxed = true) {
        every { charge(match { it.amount.value == BigDecimal(200) }) } returns true
        every { charge(match { it.amount.value == BigDecimal(402) }) } returns false
        every { charge(match { it.amount.value == BigDecimal(404) }) } throws CustomerNotFoundException(1)
        every { charge(match { it.amount.value == BigDecimal(400) }) } throws CurrencyMismatchException(1, 1)
        every { charge(match { it.amount.value == BigDecimal(503) }) } throws NetworkException()
    }
    private val failureNotificator = mockk<FailureNotificator>(relaxed = true)

    private val sut = BillingService(
        paymentProvider = paymentProvider,
        dal = dal,
        failureNotificator = failureNotificator
    )

    private fun mockInvoice(mockedResult: Int, mockedStatus: InvoiceStatus = InvoiceStatus.PENDING): Invoice {
        return Invoice(1, 1, Money(BigDecimal(mockedResult), Currency.USD), mockedStatus)
    }

    @Test
    fun `will fetch invoices with pending status`() {
        sut.handle()

        verify { dal.fetchInvoicesByStatus(InvoiceStatus.PENDING) }
        confirmVerified(dal)
    }

    @Test
    fun `will charge each invoice fetched`() {
        every { dal.fetchInvoicesByStatus(InvoiceStatus.PENDING) } returns listOf(
            mockInvoice(200),
            mockInvoice(400)
        )

        sut.handle()

        verify(exactly = 2) { paymentProvider.charge(any()) }
        confirmVerified(paymentProvider)
    }

    @Test
    fun `will mark invoices as paid when customer charged successfully`() {
        val successInvoice = spyk(mockInvoice(200))
        every { dal.fetchInvoicesByStatus(InvoiceStatus.PENDING) } returns listOf(successInvoice)

        sut.handle()

        verify { successInvoice.pay() }
    }

    @Test
    fun `will persist invoice status changes on dao`() {
        val successInvoice = mockInvoice(200)
        every { dal.fetchInvoicesByStatus(InvoiceStatus.PENDING) } returns listOf(
            successInvoice,
            mockInvoice(402)
        )

        sut.handle()

        verify(exactly = 1) { dal.updateInvoice(successInvoice) }
    }

    @Test
    fun `will notify invoice failure when occur a mismatch of currencies between customer and invoice`() {
        every { dal.fetchInvoicesByStatus(InvoiceStatus.PENDING) } returns listOf(
            mockInvoice(400),
            mockInvoice(200)
        )

        sut.handle()

        val expectedException = CurrencyMismatchException(1, 1)
        verify(exactly = 1) { failureNotificator.notify(withArg <BusinessErrorEvent> {
            assertTrue(it.resourceName == "Invoice")
            assertTrue(it.resourceId == 1)
            assertTrue(it.reason == null)
            assertTrue(it.exception?.message == expectedException.message)
        }) }
    }

    @Test
    fun `will notify invoice charge failure when the customer was not found`() {
        every { dal.fetchInvoicesByStatus(InvoiceStatus.PENDING) } returns listOf(
            mockInvoice(404),
            mockInvoice(200)
        )

        sut.handle()

        val expectedException = CustomerNotFoundException(1)
        verify(exactly = 1) { failureNotificator.notify(withArg <BusinessErrorEvent> {
            assertTrue(it.resourceName == "Invoice")
            assertTrue(it.resourceId == 1)
            assertTrue(it.reason == null)
            assertTrue(it.exception?.message == expectedException.message)
        }) }
    }

    @Test
    fun `will notify invoice failure when a network exception occurs`() {
        every { dal.fetchInvoicesByStatus(InvoiceStatus.PENDING) } returns listOf(
            mockInvoice(503),
            mockInvoice(200)
        )

        sut.handle()

        val expectedException = NetworkException()
        verify(exactly = 1) { failureNotificator.notify(withArg <ApplicationErrorEvent> {
            assertTrue(it.resourceName == "Invoice")
            assertTrue(it.resourceId == 1)
            assertTrue(it.exception.message == expectedException.message)
        }) }
    }

    @Test
    fun `will notify invoice charge failure when a invoice charge declined`() {
        every { dal.fetchInvoicesByStatus(InvoiceStatus.PENDING) } returns listOf(
            mockInvoice(402),
            mockInvoice(200)
        )

        sut.handle()

        verify(exactly = 1) { failureNotificator.notify(withArg <BusinessErrorEvent> {
            assertTrue(it.resourceName == "Invoice")
            assertTrue(it.resourceId == 1)
            assertTrue(it.reason == "Invoice charge declined due lack of account balance of customer '1'")
        }) }
    }
}
