package io.pleo.antaeus.core.external

import io.pleo.antaeus.core.events.FailureEvent

interface FailureNotificator {
    fun notify(failure: FailureEvent)
}
