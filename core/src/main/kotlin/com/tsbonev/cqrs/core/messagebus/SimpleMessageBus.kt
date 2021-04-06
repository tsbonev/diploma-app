package com.tsbonev.cqrs.core.messagebus

import com.tsbonev.cqrs.core.Command
import com.tsbonev.cqrs.core.CommandHandler
import com.tsbonev.cqrs.core.Event
import com.tsbonev.cqrs.core.EventHandler
import com.tsbonev.cqrs.core.EventWithBinaryPayload
import com.tsbonev.cqrs.core.ValidationException

/**
 * @author Tsvetozar Bonev (tsbonev@gmail.com)
 */
class SimpleMessageBus : MessageBus {

	private val commandHandlers: MutableMap<String, ValidatedCommandHandler<Command<Any>, Any>> = mutableMapOf()
	private val eventHandlers: MutableMap<String, MutableList<EventHandler<Any>>> = mutableMapOf()
	private val interceptors = mutableListOf<Interceptor>()

	override fun <T : Command<R>, R> registerCommandHandler(aClass: Class<T>, handler: CommandHandler<T,R>, validation: Validation<T>) {
		val key = aClass.name

		val commandHandler = ValidatedCommandHandler(handler, validation)
		@Suppress("UNCHECKED_CAST")
		commandHandlers[key] = commandHandler as ValidatedCommandHandler<Command<Any>, Any>
	}

	override fun <T : Event> registerEventHandler(aClass: Class<T>, handler: EventHandler<T>) {
		val key = aClass.name
		if (!eventHandlers.containsKey(key)) {
			eventHandlers[key] = mutableListOf()
		}

		@Suppress("UNCHECKED_CAST")
		eventHandlers[key]!!.add(handler as EventHandler<Any>)
	}

	override fun registerInterceptor(interceptor: Interceptor) {
		interceptors.add(interceptor)
	}

	override fun <T : Command<R>, R> send(command: T) : R  {
		val key = command::class.java.name

		if (!commandHandlers.containsKey(key)) {
			throw IllegalArgumentException("No compatible handler found.")
		}

		@Suppress("UNCHECKED_CAST")
		val handler = commandHandlers[key] as ValidatedCommandHandler<T, R>

		val errors = handler.validation.validate(command)
		if (errors.isNotEmpty()) {
			throw ValidationException(errors)
		}

		return handler.handler.handle(command)
	}

	override fun handle(event: EventWithBinaryPayload) {
		val key = event.event::class.java.name

		if (!eventHandlers.containsKey(key)) {
			interceptors.forEach { it.intercept(SimpleChain(event, listOf())) }
			return
		}

		val handlers = eventHandlers[key]!!

		if (interceptors.isNotEmpty()) {
			val chain = SimpleChain(event, handlers)
			interceptors.forEach { it.intercept(chain) }
			return
		}

		handlers.forEach {
			it.handle(event.event)
		}
	}
}