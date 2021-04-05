package com.tsbonev.cqrs.core

import com.tsbonev.cqrs.core.eventstore.EventStore
import com.tsbonev.cqrs.core.messagebus.MessageBus

/**
 * @author Tsvetozar Bonev (tsbonev@gmail.com)
 */
interface CQRS {

	fun messageBus(): MessageBus

	fun eventStore(): EventStore

	fun aggregates(): Aggregates

}