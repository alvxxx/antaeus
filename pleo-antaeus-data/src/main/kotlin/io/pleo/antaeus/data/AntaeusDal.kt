/*
    Implements the data access layer (DAL).
    The data access layer generates and executes requests to the database.

    See the `mappings` module for the conversions between database rows and Kotlin objects.
 */

package io.pleo.antaeus.data

import io.pleo.antaeus.models.Currency
import io.pleo.antaeus.models.Customer
import io.pleo.antaeus.models.Invoice
import io.pleo.antaeus.models.InvoiceStatus
import io.pleo.antaeus.models.Money
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.concurrent.atomic.AtomicInteger

class AntaeusDal(private val db: Database) {
    fun fetchInvoice(id: Int): Invoice? {
        // transaction(db) runs the internal query as a new database transaction.
        return transaction(db) {
            // Returns the first invoice with matching id.
            InvoiceTable
                .select { InvoiceTable.id.eq(id) }
                .firstOrNull()
                ?.toInvoice()
        }
    }

    suspend fun onEveryPendingInvoice(numberOfCoroutines: Int = 16, action: suspend (Invoice) -> Unit) = runBlocking {
        val currentPage = AtomicInteger(-numberOfCoroutines)
        repeat(numberOfCoroutines) {
            launch {
                do {
                    val nextPage = currentPage.addAndGet(numberOfCoroutines)
                    val invoicesToCharge = fetchInvoicePageByStatus(InvoiceStatus.PENDING, numberOfCoroutines, nextPage)
                    invoicesToCharge.forEach { invoice -> action.invoke(invoice) }
                } while (invoicesToCharge.isNotEmpty())
            }
        }
    }

    fun fetchInvoices(): List<Invoice> {
        return transaction(db) {
            InvoiceTable
                .selectAll()
                .map { it.toInvoice() }
        }
    }

    fun fetchInvoicePageByStatus(status: InvoiceStatus, take: Int, pageNumber: Int): List<Invoice> {
        return transaction(db) {
            InvoiceTable
                .select { InvoiceTable.status like status.toString() }
                .limit(take, pageNumber)
                .map { it.toInvoice() }
        }
    }

    fun updateInvoice(invoice: Invoice) {
        transaction(db){
            InvoiceTable.update({ InvoiceTable.id eq invoice.id }) {
                it[status] = invoice.status.toString()
                it[currency] = invoice.amount.currency.toString()
                it[customerId] = invoice.customerId
                it[value] = invoice.amount.value
            }
        }
    }

    fun createInvoice(amount: Money, customer: Customer, status: InvoiceStatus = InvoiceStatus.PENDING): Invoice? {
        val id = transaction(db) {
            // Insert the invoice and returns its new id.
            InvoiceTable
                .insert {
                    it[this.value] = amount.value
                    it[this.currency] = amount.currency.toString()
                    it[this.status] = status.toString()
                    it[this.customerId] = customer.id
                } get InvoiceTable.id
        }

        return fetchInvoice(id)
    }

    fun fetchCustomer(id: Int): Customer? {
        return transaction(db) {
            CustomerTable
                .select { CustomerTable.id.eq(id) }
                .firstOrNull()
                ?.toCustomer()
        }
    }

    fun fetchCustomers(): List<Customer> {
        return transaction(db) {
            CustomerTable
                .selectAll()
                .map { it.toCustomer() }
        }
    }

    fun createCustomer(currency: Currency): Customer? {
        val id = transaction(db) {
            // Insert the customer and return its new id.
            CustomerTable.insert {
                it[this.currency] = currency.toString()
            } get CustomerTable.id
        }

        return fetchCustomer(id)
    }
}
