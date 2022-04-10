package io.pleo.antaeus.core.services

import io.mockk.*
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
import java.math.BigDecimal

class BillingServiceTest {
    private val dal = mockk<AntaeusDal>(relaxed = true)
    private val paymentProvider = mockk<PaymentProvider>(relaxed = true) {
        coEvery { charge(match { it.amount.value == BigDecimal(200) }) } returns true
        coEvery { charge(match { it.amount.value == BigDecimal(402) }) } returns false
        coEvery { charge(match { it.amount.value == BigDecimal(404) }) } throws CustomerNotFoundException(1)
        coEvery { charge(match { it.amount.value == BigDecimal(400) }) } throws CurrencyMismatchException(1, 1)
        coEvery { charge(match { it.amount.value == BigDecimal(503) }) } throws NetworkException()
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
//
//    @Test
//    fun `will fetch invoices with pending status`() = runTest {
//        coEvery {dal.fetchInvoicesByStatus(InvoiceStatus.PENDING, any(), any()) } returns listOf()
//
//        sut.handle()
//
//        coVerify { dal.fetchInvoicesByStatus(InvoiceStatus.PENDING, any(), any()) }
//        confirmVerified(dal)
//    }
//
//    @Test
//    fun `will charge each invoice fetched`() = runTest {
//        coEvery { dal.fetchInvoicesByStatus(InvoiceStatus.PENDING, any(), any()) } returns listOf(
//            mockInvoice(200),
//            mockInvoice(400)
//        )
//
//        sut.handle()
//
//        coVerify(exactly = 2) { paymentProvider.charge(any()) }
//        confirmVerified(paymentProvider)
//    }
//
//    @Test
//    fun `will mark invoices as paid when customer charged successfully`() = runTest {
//        val successInvoice = spyk(mockInvoice(200))
//        coEvery { dal.fetchInvoicesByStatus(InvoiceStatus.PENDING, any(), any()) } returns listOf(successInvoice)
//
//        sut.handle()
//
//        verify { successInvoice.pay() }
//    }
//
//    @Test
//    fun `will persist invoice status changes on dao`() = runTest {
//        val successInvoice = mockInvoice(200)
//        coEvery { dal.fetchInvoicesByStatus(InvoiceStatus.PENDING, any(), any()) } returns listOf(
//            successInvoice,
//            mockInvoice(402)
//        )
//
//        sut.handle()
//
//        coVerify(exactly = 1) { dal.updateInvoice(successInvoice) }
//    }
//
//    @Test
//    fun `will notify invoice failure when occur a mismatch of currencies between customer and invoice`() = runTest {
//        coEvery { dal.fetchInvoicesByStatus(InvoiceStatus.PENDING, any(), any()) } returns listOf(
//            mockInvoice(400),
//            mockInvoice(200)
//        )
//
//        sut.handle()
//
//        val expectedException = CurrencyMismatchException(1, 1)
//        verify(exactly = 1) { failureNotificator.notify(withArg <BusinessErrorEvent> {
//            assertTrue(it.resourceName == "Invoice")
//            assertTrue(it.resourceId == 1)
//            assertTrue(it.reason == null)
//            assertTrue(it.exception?.message == expectedException.message)
//        }) }
//    }
//
//    @Test
//    fun `will notify invoice charge failure when the customer was not found`() = runTest {
//        coEvery { dal.fetchInvoicesByStatus(InvoiceStatus.PENDING, any(), any()) } returns listOf(
//            mockInvoice(404),
//            mockInvoice(200)
//        )
//
//        sut.handle()
//
//        val expectedException = CustomerNotFoundException(1)
//        verify(exactly = 1) { failureNotificator.notify(withArg <BusinessErrorEvent> {
//            assertTrue(it.resourceName == "Invoice")
//            assertTrue(it.resourceId == 1)
//            assertTrue(it.reason == null)
//            assertTrue(it.exception?.message == expectedException.message)
//        }) }
//    }
//
//    @Test
//    fun `will notify invoice failure when a network exception occurs`() = runTest {
//        coEvery { dal.fetchInvoicesByStatus(InvoiceStatus.PENDING, any(), any()) } returns listOf(
//            mockInvoice(503),
//            mockInvoice(200)
//        )
//
//        sut.handle()
//
//        val expectedException = NetworkException()
//        verify(exactly = 1) { failureNotificator.notify(withArg <ApplicationErrorEvent> {
//            assertTrue(it.resourceName == "Invoice")
//            assertTrue(it.resourceId == 1)
//            assertTrue(it.exception.message == expectedException.message)
//        }) }
//    }
//
//    @Test
//    fun `will notify invoice charge failure when a invoice charge declined`() = runTest {
//        coEvery { dal.fetchInvoicesByStatus(InvoiceStatus.PENDING, any(), any()) } returns listOf(
//            mockInvoice(402),
//            mockInvoice(200)
//        )
//
//        sut.handle()
//
//        verify(exactly = 1) { failureNotificator.notify(withArg <BusinessErrorEvent> {
//            assertTrue(it.resourceName == "Invoice")
//            assertTrue(it.resourceId == 1)
//            assertTrue(it.reason == "Invoice charge declined due lack of account balance of customer '1'")
//        }) }
//    }
}
