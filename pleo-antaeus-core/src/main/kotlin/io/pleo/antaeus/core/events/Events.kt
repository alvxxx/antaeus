package io.pleo.antaeus.core.events

abstract class FailureEvent(val resourceId: Int, val resourceName: String)

class BusinessErrorEvent(resourceId: Int, resourceName: String, val reason: String? = null, val exception: Exception? = null): FailureEvent(resourceId, resourceName)
class ApplicationErrorEvent(resourceId: Int, resourceName: String, val exception: Exception): FailureEvent(resourceId, resourceName)
