package com.tsbonev.cqrs.core.helpers

import com.tsbonev.cqrs.core.EventWithBinaryPayload
import com.tsbonev.cqrs.core.messagebus.Command
import com.tsbonev.cqrs.core.messagebus.CommandResponse
import com.tsbonev.cqrs.core.messagebus.Event
import com.tsbonev.cqrs.core.messagebus.Interceptor
import com.tsbonev.cqrs.core.messagebus.MessageBus
import com.tsbonev.cqrs.core.messagebus.SimpleMessageBus
import com.tsbonev.nharker.cqrs.Workflow

@Suppress("UNCHECKED_CAST")
class InMemoryMessageBus(private val messageBus: SimpleMessageBus = SimpleMessageBus()) : MessageBus {
	val handledEvents = mutableListOf<Event>()
	val sentCommands = mutableListOf<Command>()

	override fun registerWorkflow(workflow: Workflow) {
		messageBus.registerWorkflow(workflow)
	}

	override fun registerInterceptor(interceptor: Interceptor) {

	}

	override fun <T : Command> send(command: T): CommandResponse {
		sentCommands.add(command)

		return messageBus.send(command)
	}

	override fun publish(event: Event) {
		handledEvents.add(event)
	}
}