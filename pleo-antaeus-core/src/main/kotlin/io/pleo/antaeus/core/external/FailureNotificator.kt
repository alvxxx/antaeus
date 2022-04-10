package io.pleo.antaeus.core.external

import io.pleo.antaeus.core.events.FailureEvent

interface FailureNotificator {
    /*
       Notifies an error happened during a process
    */
    suspend fun notify(failure: FailureEvent)
}
