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
    private val paymentProvider = mockk<PaymentProvider>(relaxed = true)

    private val sut = BillingService(
        paymentProvider = paymentProvider,
        dal = dal
    )

    private fun mockInvoice(mockedResult: BigDecimal): Invoice {
        return mockk {
            every { status } returns InvoiceStatus.PENDING
            every { amount } returns Money(mockedResult, Currency.USD)
        }
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
            mockInvoice(BigDecimal.valueOf(200)),
            mockInvoice(BigDecimal.valueOf(400))
        )

        sut.handle()

        verify(exactly = 2) { paymentProvider.charge(any()) }
        confirmVerified(paymentProvider)
    }
}
