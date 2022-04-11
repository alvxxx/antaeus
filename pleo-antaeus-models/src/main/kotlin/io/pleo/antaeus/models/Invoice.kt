package io.pleo.antaeus.models

class Invoice(val id: Int, val customerId: Int, val amount: Money, status: InvoiceStatus) {
    var status: InvoiceStatus = status
    internal set
}
