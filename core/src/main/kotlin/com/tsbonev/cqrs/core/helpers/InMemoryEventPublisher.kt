package com.tsbonev.cqrs.core.helpers

import com.tsbonev.cqrs.core.PublishErrorException
import com.tsbonev.cqrs.core.eventstore.EventPublisher
import com.tsbonev.cqrs.core.eventstore.EventWithContext
import com.tsbonev.cqrs.core.messagebus.Event
import com.tsbonev.cqrs.core.snapshot.MessageFormat
import java.io.ByteArrayInputStream

class InMemoryEventPublisher(private val messageFormat: MessageFormat<Any>) : EventPublisher {
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
			messageFormat.parse(it.eventData as String, it.kind) as Event
		})
	}
}