package com.tsbonev.cqrs.core.messagebus

/**
 * Provides the method to intercept an event or command that
 * the event bus is about to process.
 *
 * @author Tsvetozar Bonev (tsbonev@gmail.com)
 */
interface Interceptor {
	interface Chain {
		fun event(): Event

		fun proceed(event: Event)
	}

	fun intercept(chain: Chain)
}

class SimpleChain(val event: Event, private val handlers: List<EventInvoker>) : Interceptor.Chain {

	override fun event(): Event {
		return event
	}

	override fun proceed(event: Event) {
		handlers.forEach {
			it.invoke(event)
		}
	}
}