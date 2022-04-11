package io.pleo.antaeus.core.services

import io.mockk.*
import io.pleo.antaeus.core.events.ApplicationErrorEvent
import io.pleo.antaeus.core.events.BusinessErrorEvent
import io.pleo.antaeus.core.events.InvoiceStatusChangedEvent
import io.pleo.antaeus.core.exceptions.CurrencyMismatchException
import io.pleo.antaeus.core.exceptions.CustomerNotFoundException
import io.pleo.antaeus.core.exceptions.NetworkException
import io.pleo.antaeus.core.external.EventNotificator
import io.pleo.antaeus.core.external.PaymentProvider
import io.pleo.antaeus.data.AntaeusDal
import io.pleo.antaeus.models.Currency
import io.pleo.antaeus.models.Invoice
import io.pleo.antaeus.models.InvoiceStatus
import io.pleo.antaeus.models.Money
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.math.BigDecimal

@OptIn(ExperimentalCoroutinesApi::class)
class BillingServiceTest {
    private val numberOfCoroutines = 2
    private val dal = mockk<AntaeusDal>(relaxed = true)
    private val paymentProvider = mockk<PaymentProvider>(relaxed = true) {
        coEvery { charge(match { it.amount.value == BigDecimal(200) }) } returns true
        coEvery { charge(match { it.amount.value == BigDecimal(402) }) } returns false
        coEvery { charge(match { it.amount.value == BigDecimal(404) }) } throws CustomerNotFoundException(1)
        coEvery { charge(match { it.amount.value == BigDecimal(400) }) } throws CurrencyMismatchException(1, 1)
        coEvery { charge(match { it.amount.value == BigDecimal(503) }) } throws NetworkException()
    }
    private val eventNotificator = mockk<EventNotificator>(relaxed = true)

    private val sut = BillingService(
        paymentProvider = paymentProvider,
        dal = dal,
        eventNotificator = eventNotificator,
        numberOfCoroutines = numberOfCoroutines
    )

    private fun mockInvoice(mockedResult: Int, mockedStatus: InvoiceStatus = InvoiceStatus.PENDING): Invoice {
        return Invoice(1, 1, Money(BigDecimal(mockedResult), Currency.USD), mockedStatus)
    }

    @Test
    fun `will fetch all invoices with pending status`() = runTest {
        every { dal.fetchInvoicePageByStatus(InvoiceStatus.PENDING, numberOfCoroutines, 0) } returns listOf(
            mockInvoice(200),
            mockInvoice(400)
        )
        every { dal.fetchInvoicePageByStatus(InvoiceStatus.PENDING, numberOfCoroutines, 2) } returns listOf(
            mockInvoice(200),
            mockInvoice(200)
        )
        every { dal.fetchInvoicePageByStatus(InvoiceStatus.PENDING, numberOfCoroutines, 4) } returns listOf(
            mockInvoice(402),
        )
        every { dal.fetchInvoicePageByStatus(InvoiceStatus.PENDING, numberOfCoroutines, match { it > 4 }) } returns listOf()

        sut.chargeInvoices()

        verify(exactly = 1) {
            dal.fetchInvoicePageByStatus(InvoiceStatus.PENDING, numberOfCoroutines, 0)
            dal.fetchInvoicePageByStatus(InvoiceStatus.PENDING, numberOfCoroutines, 2)
            dal.fetchInvoicePageByStatus(InvoiceStatus.PENDING, numberOfCoroutines, 4)
            dal.fetchInvoicePageByStatus(InvoiceStatus.PENDING, numberOfCoroutines, 6)
        }
    }

    @Test
    fun `will charge each invoice fetched`() = runTest {
        every { dal.fetchInvoicePageByStatus(InvoiceStatus.PENDING, numberOfCoroutines, 0) } returns listOf(
            mockInvoice(200),
            mockInvoice(400)
        )
        every { dal.fetchInvoicePageByStatus(InvoiceStatus.PENDING, numberOfCoroutines, match { it > 2 }) } returns listOf()

        sut.chargeInvoices()

        coVerify(exactly = 2) { paymentProvider.charge(any()) }
        confirmVerified(paymentProvider)
    }

    @Test
    fun `will mark invoices as paid when customer charged successfully`() = runTest {
        val successInvoice = spyk(mockInvoice(200))
        every { dal.fetchInvoicePageByStatus(InvoiceStatus.PENDING, numberOfCoroutines, 0) } returns listOf(successInvoice)
        every { dal.fetchInvoicePageByStatus(InvoiceStatus.PENDING, numberOfCoroutines, match { it > 2 }) } returns listOf()

        sut.chargeInvoices()

        verify { successInvoice.pay() }
        coVerify(exactly = 1) { eventNotificator.notify(withArg <InvoiceStatusChangedEvent> {
            assertTrue(it.resourceName == "Invoice")
            assertTrue(it.resourceId == 1)
            assertTrue(it.oldStatus == "PENDING")
            assertTrue(it.newStatus == "PAID")
        }) }
    }

    @Test
    fun `will notify invoice changes to paid`() = runTest {
        val successInvoice = spyk(mockInvoice(200))
        every { dal.fetchInvoicePageByStatus(InvoiceStatus.PENDING, numberOfCoroutines, 0) } returns listOf(successInvoice)
        every { dal.fetchInvoicePageByStatus(InvoiceStatus.PENDING, numberOfCoroutines, match { it > 2 }) } returns listOf()

        sut.chargeInvoices()

        coVerify(exactly = 1) { eventNotificator.notify(withArg <InvoiceStatusChangedEvent> {
            assertTrue(it.resourceName == "Invoice")
            assertTrue(it.resourceId == 1)
            assertTrue(it.oldStatus == "PENDING")
            assertTrue(it.newStatus == "PAID")
        }) }
    }

    @Test
    fun `will mark invoices as overdue successfully`() = runTest {
        val successInvoice1 = spyk(mockInvoice(200))
        val successInvoice2 = spyk(mockInvoice(200))
        every { dal.fetchInvoicePageByStatus(InvoiceStatus.PENDING, numberOfCoroutines, 0) } returns listOf(
            successInvoice1,
            successInvoice2
        )
        every { dal.fetchInvoicePageByStatus(InvoiceStatus.PENDING, numberOfCoroutines, match { it > 2 }) } returns listOf()

        sut.markPendingInvoicesAsOverdue()

        verify {
            successInvoice1.overdue()
            successInvoice2.overdue()
        }
    }

    @Test
    fun `will notify invoice changes to overdue`() = runTest {
        val successInvoice1 = spyk(mockInvoice(200))
        val successInvoice2 = spyk(mockInvoice(200))
        every { dal.fetchInvoicePageByStatus(InvoiceStatus.PENDING, numberOfCoroutines, 0) } returns listOf(
            successInvoice1,
            successInvoice2
        )
        every { dal.fetchInvoicePageByStatus(InvoiceStatus.PENDING, numberOfCoroutines, match { it > 2 }) } returns listOf()

        sut.markPendingInvoicesAsOverdue()

        coVerify(exactly = 2) { eventNotificator.notify(withArg <InvoiceStatusChangedEvent> {
            assertTrue(it.resourceName == "Invoice")
            assertTrue(it.resourceId == 1)
            assertTrue(it.oldStatus == "PENDING")
            assertTrue(it.newStatus == "OVERDUE")
        }) }
    }

    @Test
    fun `will persist invoice status changes on dao`() = runTest {
        val successInvoice = mockInvoice(200)
        every { dal.fetchInvoicePageByStatus(InvoiceStatus.PENDING, numberOfCoroutines, 0) } returns listOf(
            successInvoice,
            mockInvoice(402)
        )
        every { dal.fetchInvoicePageByStatus(InvoiceStatus.PENDING, numberOfCoroutines, match { it > 2 }) } returns listOf()

        sut.chargeInvoices()

        verify(exactly = 1) { dal.updateInvoice(successInvoice) }
    }

    @Test
    fun `will notify invoice failure when occur a mismatch of currencies between customer and invoice`() = runTest {
        val failInvoice = spyk(mockInvoice(400))
        every { dal.fetchInvoicePageByStatus(InvoiceStatus.PENDING, numberOfCoroutines, 0) } returns listOf(
            failInvoice,
            mockInvoice(200)
        )
        every { dal.fetchInvoicePageByStatus(InvoiceStatus.PENDING, numberOfCoroutines, match { it > 2 }) } returns listOf()

        sut.chargeInvoices()

        val expectedException = CurrencyMismatchException(1, 1)
        coVerify(exactly = 1) { eventNotificator.notify(withArg <BusinessErrorEvent> {
            assertTrue(it.resourceName == "Invoice")
            assertTrue(it.resourceId == 1)
            assertTrue(it.reason == null)
            assertTrue(it.exception?.message == expectedException.message)
        }) }
        verify(exactly = 1) { failInvoice.uncollect() }
        verify(exactly = 1) { dal.updateInvoice(failInvoice) }
    }

    @Test
    fun `will notify invoice charge failure when the customer was not found`() = runTest {
        val failInvoice = spyk(mockInvoice(404))
        every { dal.fetchInvoicePageByStatus(InvoiceStatus.PENDING, numberOfCoroutines, 0) } returns listOf(
            failInvoice,
            mockInvoice(200)
        )
        every { dal.fetchInvoicePageByStatus(InvoiceStatus.PENDING, numberOfCoroutines, match { it > 2 }) } returns listOf()

        sut.chargeInvoices()

        val expectedException = CustomerNotFoundException(1)
        coVerify(exactly = 1) { eventNotificator.notify(withArg <BusinessErrorEvent> {
            assertTrue(it.resourceName == "Invoice")
            assertTrue(it.resourceId == 1)
            assertTrue(it.reason == null)
            assertTrue(it.exception?.message == expectedException.message)
        }) }
        verify(exactly = 1) { failInvoice.uncollect() }
        verify(exactly = 1) { dal.updateInvoice(failInvoice) }
    }

    @Test
    fun `will notify invoice failure when a network exception occurs`() = runTest {
        every { dal.fetchInvoicePageByStatus(InvoiceStatus.PENDING, numberOfCoroutines, 0) } returns listOf(
            mockInvoice(503),
            mockInvoice(200)
        )
        every { dal.fetchInvoicePageByStatus(InvoiceStatus.PENDING, numberOfCoroutines, match { it > 2 }) } returns listOf()

        sut.chargeInvoices()

        val expectedException = NetworkException()
        coVerify(exactly = 1) { eventNotificator.notify(withArg <ApplicationErrorEvent> {
            assertTrue(it.resourceName == "Invoice")
            assertTrue(it.resourceId == 1)
            assertTrue(it.exception.message == expectedException.message)
        }) }
    }

    @Test
    fun `will notify invoice charge failure when a invoice charge declined`() = runTest {
        every { dal.fetchInvoicePageByStatus(InvoiceStatus.PENDING, numberOfCoroutines, 0) } returns listOf(
            mockInvoice(402),
            mockInvoice(200)
        )
        every { dal.fetchInvoicePageByStatus(InvoiceStatus.PENDING, numberOfCoroutines, match { it > 2 }) } returns listOf()

        sut.chargeInvoices()

        coVerify(exactly = 1) { eventNotificator.notify(withArg <BusinessErrorEvent> {
            assertTrue(it.resourceName == "Invoice")
            assertTrue(it.resourceId == 1)
            assertTrue(it.reason == "Invoice charge declined due lack of account balance of customer '1'")
        }) }
    }
}
