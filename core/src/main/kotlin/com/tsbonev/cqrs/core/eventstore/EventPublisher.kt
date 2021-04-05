package com.tsbonev.cqrs.core.eventstore

import com.tsbonev.cqrs.core.EventWithBinaryPayload
import com.tsbonev.cqrs.core.PublishErrorException

/**
 * @author Tsvetozar Bonev (tsbonev@gmail.com)
 */
interface EventPublisher {
	@Throws(PublishErrorException::class)
	fun publish(events: Iterable<EventWithBinaryPayload>)
}