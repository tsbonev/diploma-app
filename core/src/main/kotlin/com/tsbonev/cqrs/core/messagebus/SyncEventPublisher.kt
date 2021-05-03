package com.tsbonev.cqrs.core.messagebus

import com.tsbonev.cqrs.core.PublishErrorException
import com.tsbonev.cqrs.core.eventstore.EventPublisher
import com.tsbonev.cqrs.core.eventstore.EventWithContext
import com.tsbonev.cqrs.core.snapshot.MessageFormat
import java.io.ByteArrayInputStream


class SyncEventPublisher(private val messageBus: MessageBus, private val messageFormat: MessageFormat<Any>) : EventPublisher {
	override fun publish(events: Iterable<EventWithContext>) {
		events.forEach {
			try {
				messageBus.publish(messageFormat.parse(it.eventData, it.kind))
			} catch (ex: Exception) {
				throw PublishErrorException(ex)
			}
		}
	}
}