package com.tsbonev.cqrs.core.helpers

import com.tsbonev.cqrs.core.PublishErrorException
import com.tsbonev.cqrs.core.eventstore.EventPublisher
import com.tsbonev.cqrs.core.eventstore.EventWithContext
import com.tsbonev.cqrs.core.messagebus.Event
import com.tsbonev.cqrs.core.snapshot.MessageFormat
import java.io.ByteArrayInputStream

class InMemoryEventPublisher(private val messageFormat: MessageFormat) : EventPublisher {
	var events = mutableListOf<Event>()
	private var nextPublishFailsWithError = false

	fun pretendThatNextPublishWillFail() {
		nextPublishFailsWithError = true
	}

	override fun publish(events: Iterable<EventWithContext>) {
		if (nextPublishFailsWithError) {
			nextPublishFailsWithError = false
			throw PublishErrorException()
		}
		this.events.addAll(events.map {
			messageFormat.parse(ByteArrayInputStream(it.eventData), it.kind) as Event
		})
	}
}