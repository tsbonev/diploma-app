package com.tsbonev.cqrs.core.helpers

import com.tsbonev.cqrs.core.EventWithBinaryPayload
import com.tsbonev.cqrs.core.PublishErrorException
import com.tsbonev.cqrs.core.eventstore.EventPublisher

class InMemoryEventPublisher : EventPublisher {
	var events = mutableListOf<EventWithBinaryPayload>()
	var nextPublishFailsWithError = false

	override fun publish(events: Iterable<EventWithBinaryPayload>) {
		if (nextPublishFailsWithError) {
			nextPublishFailsWithError = false
			throw PublishErrorException()
		}
		this.events.addAll(events)
	}

	fun cleanUp() {
		events = mutableListOf()
	}

	fun pretendThatNextPublishWillFail() {
		nextPublishFailsWithError = true
	}
}