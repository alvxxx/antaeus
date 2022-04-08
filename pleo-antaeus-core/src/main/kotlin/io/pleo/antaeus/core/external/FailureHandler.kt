package io.pleo.antaeus.core.external

import io.pleo.antaeus.core.events.FailureEvent

interface FailureHandler {
    fun notify(failure: FailureEvent)
}
