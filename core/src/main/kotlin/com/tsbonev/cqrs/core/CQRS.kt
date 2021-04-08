package com.tsbonev.cqrs.core

import com.tsbonev.cqrs.core.eventstore.EventStore
import com.tsbonev.cqrs.core.messagebus.MessageBus


interface CQRS {

	fun messageBus(): MessageBus

	fun eventStore(): EventStore

	fun aggregates(): Aggregates

}