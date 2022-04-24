package io.pleo.antaeus.data

import io.mockk.*
import io.pleo.antaeus.models.Currency
import io.pleo.antaeus.models.Invoice
import io.pleo.antaeus.models.InvoiceStatus
import io.pleo.antaeus.models.Money
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import java.math.BigDecimal

@OptIn(ExperimentalCoroutinesApi::class)
class AntaeusDalTest {
    private val numberOfCoroutines = 2
    private val sut = mockk<AntaeusDal> {
        coEvery { onEveryPendingInvoice(any(), any()) } answers { callOriginal() }
    }

    private fun mockInvoice(mockedResult: Int, mockedStatus: InvoiceStatus = InvoiceStatus.PENDING): Invoice {
        return Invoice(1, 1, Money(BigDecimal(mockedResult), Currency.USD), mockedStatus)
    }

    @Test
    fun `will fetch all invoices with pending status`() = runTest {
        coEvery { sut.fetchInvoicePageByStatus(InvoiceStatus.PENDING, numberOfCoroutines, 0) } returns listOf(
            mockInvoice(200),
            mockInvoice(400)
        )
        coEvery { sut.fetchInvoicePageByStatus(InvoiceStatus.PENDING, numberOfCoroutines, 2) } returns listOf(
            mockInvoice(200),
            mockInvoice(200)
        )
        coEvery { sut.fetchInvoicePageByStatus(InvoiceStatus.PENDING, numberOfCoroutines, 4) } returns listOf(
            mockInvoice(402),
        )
        coEvery { sut.fetchInvoicePageByStatus(InvoiceStatus.PENDING, numberOfCoroutines, match { it > 4 }) } returns listOf()

        runBlocking { sut.onEveryPendingInvoice(2) { } }

        coVerify(exactly = 1) {
            sut.fetchInvoicePageByStatus(InvoiceStatus.PENDING, numberOfCoroutines, 0)
            sut.fetchInvoicePageByStatus(InvoiceStatus.PENDING, numberOfCoroutines, 2)
            sut.fetchInvoicePageByStatus(InvoiceStatus.PENDING, numberOfCoroutines, 4)
            sut.fetchInvoicePageByStatus(InvoiceStatus.PENDING, numberOfCoroutines, 6)
        }
    }

    @Test
    fun `will invoke each invoice fetched`() = runTest {
        val invoiceSpy = spyk(mockInvoice(200))
        coEvery { sut.fetchInvoicePageByStatus(InvoiceStatus.PENDING, numberOfCoroutines, 0) } returns listOf(
            invoiceSpy,
            invoiceSpy
        )
        coEvery { sut.fetchInvoicePageByStatus(InvoiceStatus.PENDING, numberOfCoroutines, 2) } returns listOf(
            invoiceSpy
        )
        coEvery { sut.fetchInvoicePageByStatus(InvoiceStatus.PENDING, numberOfCoroutines, match { it > 2 }) } returns listOf()

        runBlocking { sut.onEveryPendingInvoice(2) {  invoiceSpy.status  } }

        verify(exactly = 3) { invoiceSpy.status }
    }
}
