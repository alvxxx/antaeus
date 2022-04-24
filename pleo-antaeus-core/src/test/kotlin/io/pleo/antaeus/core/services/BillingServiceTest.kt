package io.pleo.antaeus.core.services

import io.mockk.*
import io.pleo.antaeus.core.exceptions.CurrencyMismatchException
import io.pleo.antaeus.core.exceptions.CustomerNotFoundException
import io.pleo.antaeus.core.exceptions.InvoiceNotFoundException
import io.pleo.antaeus.core.exceptions.NetworkException
import io.pleo.antaeus.core.external.PaymentProvider
import io.pleo.antaeus.data.AntaeusDal
import io.pleo.antaeus.models.Currency
import io.pleo.antaeus.models.Invoice
import io.pleo.antaeus.models.InvoiceStatus
import io.pleo.antaeus.models.Money
import io.pleo.antaeus.services.InvoiceDomainService
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import java.math.BigDecimal

@OptIn(ExperimentalCoroutinesApi::class)
class BillingServiceTest {
    private val dal = mockk<AntaeusDal>(relaxed = true)
    private val paymentProvider = mockk<PaymentProvider>(relaxed = true) {
        coEvery { charge(match { it.amount.value == BigDecimal(200) }) } returns true
        coEvery { charge(match { it.amount.value == BigDecimal(402) }) } returns false
        coEvery { charge(match { it.amount.value == BigDecimal(404) }) } throws CustomerNotFoundException(1)
        coEvery { charge(match { it.amount.value == BigDecimal(400) }) } throws CurrencyMismatchException(1, 1)
        coEvery { charge(match { it.amount.value == BigDecimal(503) }) } throws NetworkException()
    }
    private val domainService = mockk<InvoiceDomainService>(relaxed = true) {
        coEvery {  uncollect(invoice = any(), exception = any()) } just Runs
        coEvery {  fail(id = any(), exception = any()) } just Runs
    }
    private val sut = BillingService(paymentProvider, dal, domainService)
    private fun mockInvoice(mockedResult: Int): Invoice {
        return Invoice(1, 1, Money(BigDecimal(mockedResult), Currency.USD), InvoiceStatus.PENDING)
    }

    @Test
    fun `will persist update invoice after charge`() = runTest {
        val invoice = mockInvoice(200)

        sut.charge(invoice)

        coVerify(exactly = 1) { dal.updateInvoice(invoice) }
    }

    @Test
    fun `will persist update invoice after overdue`() = runTest {
        val invoice = mockInvoice(200)

        sut.overdue(invoice)

        coVerify(exactly = 1) { dal.updateInvoice(invoice) }
    }

    @Test
    fun `will pay invoice if it was charged successfully`() = runTest {
        val invoice = mockInvoice(200)

        sut.charge(invoice)

        coVerify(exactly = 1) { domainService.pay(invoice) }
    }

    @Test
    fun `will overdue invoice successfully`() = runTest {
        val invoice = mockInvoice(200)

        sut.overdue(invoice)

        coVerify(exactly = 1) { domainService.overdue(invoice) }
    }

    @Test
    fun `will decline invoice if it was not charged`() = runTest {
        val invoice = mockInvoice(402)

        sut.charge(invoice)

        coVerify(exactly = 1) { domainService.decline(invoice) }
    }

    @Test
    fun `will uncollect invoice if the customer was not found`() = runTest {
        val invoice = mockInvoice(404)

        sut.charge(invoice)

        coVerify(exactly = 1) { domainService.uncollect(invoice, ofType<CustomerNotFoundException>()) }
    }

    @Test
    fun `will uncollect if the invoice currency not matches with customer currency`() = runTest {
        val invoice = mockInvoice(400)

        sut.charge(invoice)

        coVerify(exactly = 1) { domainService.uncollect(invoice, ofType<CurrencyMismatchException>()) }
    }

    @Test
    fun `will fail invoice charge if a network exception occurs`() = runTest {
        val invoice = mockInvoice(503)

        sut.charge(invoice)

        coVerify(exactly = 1) { domainService.fail(invoice.id, ofType<NetworkException>()) }
    }

    @Test
    fun `will charge invoice if invoice exists`() = runTest {
        val invoice = mockInvoice(200)
        every { dal.fetchInvoice(1) } returns invoice

        sut.chargeInvoiceById(1)

        coVerify(exactly = 1) { sut.charge(invoice) }
    }

    @Test
    fun `will fail invoice if invoice not exists`() = runTest {
        coEvery { dal.fetchInvoice(1) } returns null

        sut.chargeInvoiceById(1)

        coVerify(exactly = 1) { domainService.fail(1, ofType<InvoiceNotFoundException>()) }
    }
}
