package com.tsbonev.cqrs.core.helpers

import com.tsbonev.cqrs.core.Command
import com.tsbonev.cqrs.core.CommandHandler
import com.tsbonev.cqrs.core.Event
import com.tsbonev.cqrs.core.EventHandler
import com.tsbonev.cqrs.core.EventWithBinaryPayload
import com.tsbonev.cqrs.core.messagebus.Interceptor
import com.tsbonev.cqrs.core.messagebus.MessageBus
import com.tsbonev.cqrs.core.messagebus.ValidatedCommandHandler
import com.tsbonev.cqrs.core.messagebus.Validation

@Suppress("UNCHECKED_CAST")
class InMemoryMessageBus() : MessageBus {
	val handledEvents = mutableListOf<EventWithBinaryPayload>()
	val sentCommands = mutableListOf<Command<Any>>()


	private val commandHandlers: MutableMap<String, ValidatedCommandHandler<Command<Any>, Any>> = mutableMapOf()


	override fun handle(event: EventWithBinaryPayload) {
		handledEvents.add(event)
	}

	override fun <T : Command<V>, V> registerCommandHandler(aClass: Class<T>, handler: CommandHandler<T, V>, validation: Validation<T>) {
		val key = aClass.name

		val commandHandler = ValidatedCommandHandler(handler, validation)
		@Suppress("UNCHECKED_CAST")
		commandHandlers[key] = commandHandler as ValidatedCommandHandler<Command<Any>, Any>

	}

	override fun <T : Event> registerEventHandler(aClass: Class<T>, handler: EventHandler<T>) {

	}


	override fun <T : Command<V>, V> send(command: T): V {
		sentCommands.add(command as Command<Any>)

		val key = command::class.java.name

		if (!commandHandlers.containsKey(key)) {
			throw IllegalArgumentException("No proper handler found!")
		}

		val handler = commandHandlers[key] as ValidatedCommandHandler<T, V>

		return handler.handler.handle(command)

	}

	override fun registerInterceptor(interceptor: Interceptor) {

	}
}