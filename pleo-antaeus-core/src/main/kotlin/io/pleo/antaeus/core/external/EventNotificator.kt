package io.pleo.antaeus.core.external

import io.pleo.antaeus.core.events.Event

interface EventNotificator {
    /*
       Notifies an error happened during a process
    */
    suspend fun notify(event: Event)
}
