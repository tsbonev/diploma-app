package com.tsbonev.cqrs.core.helpers

import com.tsbonev.cqrs.core.PublishErrorException
import com.tsbonev.cqrs.core.eventstore.EventPublisher
import com.tsbonev.cqrs.core.messagebus.Event

class InMemoryEventPublisher : EventPublisher {
	var events = mutableListOf<Event>()
	var nextPublishFailsWithError = false

	override fun publish(events: Iterable<Event>) {
		if (nextPublishFailsWithError) {
			nextPublishFailsWithError = false
			throw PublishErrorException()
		}
		this.events.addAll(events)
	}

	fun pretendThatNextPublishWillFail() {
		nextPublishFailsWithError = true
	}
}