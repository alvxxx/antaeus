package io.pleo.antaeus.core.services

import io.mockk.confirmVerified
import io.mockk.mockk
import io.mockk.verify
import io.pleo.antaeus.core.external.PaymentProvider
import io.pleo.antaeus.data.AntaeusDal
import io.pleo.antaeus.models.InvoiceStatus
import org.junit.jupiter.api.Test

class BillingServiceTest {
    private val dal = mockk<AntaeusDal>(relaxed = true)
    private val paymentProvider = mockk<PaymentProvider>()

    private val sut = BillingService(
        paymentProvider = paymentProvider,
        dal = dal
    )

    @Test
    fun `will fetch invoices with pending status`() {
        sut.handle()

        verify { dal.fetchInvoicesByStatus(InvoiceStatus.PENDING) }
        confirmVerified(dal)
    }
}
