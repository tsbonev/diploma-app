package com.tsbonev.cqrs.core.eventstore

import com.tsbonev.cqrs.core.PublishErrorException
import com.tsbonev.cqrs.core.messagebus.Event

interface EventPublisher {
	@Throws(PublishErrorException::class)
	fun publish(events: Iterable<EventWithContext>)
}