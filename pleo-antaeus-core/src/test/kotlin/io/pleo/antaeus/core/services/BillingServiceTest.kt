package io.pleo.antaeus.core.services

import io.mockk.*
import io.pleo.antaeus.core.external.PaymentProvider
import io.pleo.antaeus.data.AntaeusDal
import io.pleo.antaeus.models.Currency
import io.pleo.antaeus.models.Invoice
import io.pleo.antaeus.models.InvoiceStatus
import io.pleo.antaeus.models.Money
import org.junit.jupiter.api.Test
import java.math.BigDecimal

class BillingServiceTest {
    private val dal = mockk<AntaeusDal>(relaxed = true)
    private val paymentProvider = mockk<PaymentProvider>(relaxed = true) {
        every { charge(match { it.amount.value == BigDecimal(200) }) } returns true
        every { charge(match { it.amount.value == BigDecimal(402) }) } returns false
    }

    private val sut = BillingService(
        paymentProvider = paymentProvider,
        dal = dal
    )

    private fun mockInvoice(mockedResult: BigDecimal, mockedStatus: InvoiceStatus = InvoiceStatus.PENDING): Invoice {
        return Invoice(1, 1, Money(mockedResult, Currency.USD), mockedStatus)
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
            mockInvoice(BigDecimal(200)),
            mockInvoice(BigDecimal(400))
        )

        sut.handle()

        verify(exactly = 2) { paymentProvider.charge(any()) }
        confirmVerified(paymentProvider)
    }

    @Test
    fun `will mark invoices to paid when charged successfully`() {
        val successInvoice = mockInvoice(BigDecimal(200))
        every { dal.fetchInvoicesByStatus(InvoiceStatus.PENDING) } returns listOf(successInvoice)

        sut.handle()

        verify { successInvoice.pay() }
    }

    @Test
    fun `will update invoice status`() {
        val successInvoice = mockInvoice(BigDecimal.valueOf(200))
        every { dal.fetchInvoicesByStatus(InvoiceStatus.PENDING) } returns listOf(
            successInvoice,
            mockInvoice(BigDecimal.valueOf(402))
        )

        sut.handle()

        verify(exactly = 1) { dal.updateInvoice(successInvoice) }
    }
}
