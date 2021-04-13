package com.tsbonev.cqrs.core.messagebus

import com.tsbonev.cqrs.core.BinaryPayload
import com.tsbonev.cqrs.core.EventWithBinaryPayload
import com.tsbonev.cqrs.core.helpers.InMemoryMessageBus
import com.tsbonev.nharker.cqrs.StatusCode
import com.tsbonev.nharker.cqrs.Workflow
import org.hamcrest.CoreMatchers
import org.junit.Test
import org.hamcrest.CoreMatchers.`is` as Is
import org.junit.Assert.assertThat

class InMemoryMessageBusTest {

	@Test(expected = NoHandlersInWorkflowException::class)
	fun `Handles commands when no set handler`() {
		val messageBus = InMemoryMessageBus()
		val command = DummyCommand()
		messageBus.send(command)
	}

	@Test
	fun `Handles commands`() {
		val messageBus = InMemoryMessageBus()
		val command = DummyCommand()
		messageBus.registerWorkflow(DummyWorkflow())
		messageBus.send(command)

		assertThat(messageBus.sentCommands[0], Is(CoreMatchers.equalTo(command as Command)))
	}

	@Test
	fun `Handles events`() {
		val messageBus = InMemoryMessageBus()
		messageBus.publish(DummyEvent())
		assertThat(
			messageBus.handledEvents[0],
			Is(CoreMatchers.equalTo(DummyEvent()))
		)
	}


	class DummyWorkflow : Workflow {

		@CommandHandler
		fun handle(command: DummyCommand) : CommandResponse {
			return CommandResponse(StatusCode.OK)
		}

		@EventHandler
		fun handle(event: Event) {

		}
	}

	data class DummyCommand(val value: Int = 1) : Command
	data class DummyEvent(val value: Int = 1) : Event
}