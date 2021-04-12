package com.tsbonev.cqrs.core.messagebus

import com.tsbonev.cqrs.core.BinaryPayload
import com.tsbonev.cqrs.core.Command
import com.tsbonev.cqrs.core.CommandHandler
import com.tsbonev.cqrs.core.Event
import com.tsbonev.cqrs.core.EventWithBinaryPayload
import com.tsbonev.cqrs.core.helpers.InMemoryMessageBus
import org.hamcrest.CoreMatchers
import org.junit.Test
import org.hamcrest.CoreMatchers.`is` as Is
import org.junit.Assert.assertThat

class InMemoryMessageBusTest {

	@Test(expected = IllegalArgumentException::class)
	fun `Handles commands when no set handler`() {
		val messageBus = InMemoryMessageBus()
		val command = DummyCommand()
		messageBus.send(command)
	}

	@Test
	fun `Handles commands`() {
		val messageBus = InMemoryMessageBus()
		val command = DummyCommand()
		messageBus.registerCommandHandler(DummyCommand::class.java, DummyCommandHandler())
		messageBus.send(command)

		assertThat(messageBus.sentCommands[0], Is(CoreMatchers.equalTo(command as Command<Any>)))
	}

	@Test
	fun `Handles events`() {
		val messageBus = InMemoryMessageBus()
		messageBus.handle(EventWithBinaryPayload(DummyEvent(), BinaryPayload("::payload::")))
		assertThat(
			messageBus.handledEvents[0].payload.payload,
			Is(CoreMatchers.equalTo("::payload::".toByteArray()))
		)
	}

	class DummyCommand : Command<String>
	class DummyEvent : Event
	class DummyCommandHandler : CommandHandler<DummyCommand, String> {
		override fun handle(command: DummyCommand) = "OK"
	}
}