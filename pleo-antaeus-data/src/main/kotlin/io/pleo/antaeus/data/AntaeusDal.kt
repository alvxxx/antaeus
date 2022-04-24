/*
    Implements the data access layer (DAL).
    The data access layer generates and executes requests to the database.

    See the `mappings` module for the conversions between database rows and Kotlin objects.
 */

package io.pleo.antaeus.data

import io.pleo.antaeus.models.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.concurrent.atomic.AtomicInteger

class AntaeusDal(private val db: Database) {
    fun fetchInvoice(id: Int): Invoice? =
        transaction(db) {
            InvoiceTable
                .select { InvoiceTable.id.eq(id) }
                .firstOrNull()
                ?.toInvoice()
        }

    fun fetchInvoices(): List<Invoice> =
        transaction(db) {
            InvoiceTable
                .selectAll()
                .map { it.toInvoice() }
        }

    suspend fun onEveryPendingInvoice(numberOfCoroutines: Int = 16, action: suspend (Invoice) -> Unit) = coroutineScope {
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

    internal suspend fun fetchInvoicePageByStatus(status: InvoiceStatus, take: Int, pageNumber: Int): List<Invoice> =
        withContext(Dispatchers.IO) {
            transaction(db) {
                InvoiceTable
                    .select { InvoiceTable.status eq status.toString() }
                    .limit(take, pageNumber)
                    .map { it.toInvoice() }
            }
        }

    suspend fun updateInvoice(invoice: Invoice) =
        withContext(Dispatchers.IO) {
            transaction(db) {
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

    fun fetchCustomer(id: Int): Customer? =
        transaction(db) {
            CustomerTable
                .select { CustomerTable.id.eq(id) }
                .firstOrNull()
                ?.toCustomer()
        }

    fun fetchCustomers(): List<Customer> =
        transaction(db) {
            CustomerTable
                .selectAll()
                .map { it.toCustomer() }
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
