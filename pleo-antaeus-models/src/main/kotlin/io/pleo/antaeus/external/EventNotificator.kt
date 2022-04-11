package io.pleo.antaeus.external

import io.pleo.antaeus.events.Event

interface EventNotificator {
    /*
       Notifies an error happened during a process
    */
    suspend fun notify(event: Event)
}
