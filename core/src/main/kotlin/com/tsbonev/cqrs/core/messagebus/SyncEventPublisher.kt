package com.tsbonev.cqrs.core.messagebus

import com.tsbonev.cqrs.core.PublishErrorException
import com.tsbonev.cqrs.core.eventstore.EventPublisher


class SyncEventPublisher(private val messageBus: MessageBus) : EventPublisher {
	override fun publish(events: Iterable<Event>) {
		events.forEach {
			try {
				messageBus.publish(it)
			} catch (ex: Exception) {
				throw PublishErrorException(ex)
			}
		}
	}
}