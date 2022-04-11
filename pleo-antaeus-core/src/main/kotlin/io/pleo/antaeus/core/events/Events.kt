package io.pleo.antaeus.core.events

abstract class Event(val resourceId: Int, val resourceName: String)

class BusinessErrorEvent(resourceId: Int, resourceName: String, val reason: String? = null, val exception: Exception? = null): Event(resourceId, resourceName)
class ApplicationErrorEvent(resourceId: Int, resourceName: String, val exception: Exception): Event(resourceId, resourceName)
class InvoiceStatusChangedEvent(resourceId: Int, resourceName: String, val oldStatus: String, val newStatus: String): Event(resourceId, resourceName)
