package com.tsbonev.cqrs.core.messagebus

import com.tsbonev.cqrs.core.EventHandler
import com.tsbonev.cqrs.core.EventWithBinaryPayload

/**
 * @author Tsvetozar Bonev (tsbonev@gmail.com)
 */
class SimpleChain(val event: EventWithBinaryPayload, private val eventHandlers: List<EventHandler<Any>>) : Interceptor.Chain {

	override fun event(): EventWithBinaryPayload {
		return event
	}

	override fun proceed(event: EventWithBinaryPayload) {
		eventHandlers.forEach {
			it.handle(event.event)
		}
	}

}