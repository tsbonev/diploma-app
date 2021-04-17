package com.tsbonev.cqrs.core.messagebus

import com.tsbonev.nharker.cqrs.StatusCode
import com.tsbonev.nharker.cqrs.Workflow
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.CoreMatchers.nullValue
import java.util.UUID
import org.hamcrest.CoreMatchers.`is` as Is
import org.hamcrest.MatcherAssert.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows


class SimpleMessageBusTest {

	@Test
	fun `Handles event with a single handler`() {
		val msgBus = SimpleMessageBus()

		val handler = DummyWorkflow()
		msgBus.registerWorkflow(handler)

		val event = DummyEvent(UUID.randomUUID())
		msgBus.publish(event)

		assertThat(handler.lastEvent, Is(equalTo(event)))
	}

	@Test
	fun `Handles event with multiple handler`() {
		val msgBus = SimpleMessageBus()

		val firstHandler = DummyWorkflow()
		val secondHandler = SecondDummyWorkflow()

		msgBus.registerWorkflow(firstHandler)
		msgBus.registerWorkflow(secondHandler)

		val event = DummyEvent(UUID.randomUUID())
		msgBus.publish(event)

		assertThat(firstHandler.lastEvent, Is(equalTo(event)))
		assertThat(secondHandler.lastEvent, Is(equalTo(event)))

	}

	@Test
	fun `No handlers are set`() {
		val msgBus = SimpleMessageBus()
		msgBus.publish(DummyEvent(UUID.randomUUID()))
	}

	@Test
	fun `Handles command with command handler`() {
		val msgBus = SimpleMessageBus()

		val handler = DummyWorkflow()
		msgBus.registerWorkflow(handler)

		val dummyCommand = DummyCommand("::test::")
		msgBus.send(dummyCommand)

		assertThat(handler.lastCommand, Is(dummyCommand))
	}

	@Test
	fun `Handles command with command handler and returns a response`() {
		val msgBus = SimpleMessageBus()

		val handler = DummyWorkflow()
		msgBus.registerWorkflow(handler)

		val dummyCommand = DummyCommand("::test::")
		val response = msgBus.send(dummyCommand)

		assertThat(response, Is(CommandResponse(StatusCode.OK)))
		assertThat(handler.lastCommand, Is(dummyCommand))
	}

	@Test
	fun `No proper handler is set`() {
		val msgBus = SimpleMessageBus()

		assertThrows<NoHandlersInWorkflowException> {
		val dummyCommand = DummyCommand("::test::")
		msgBus.send(dummyCommand)
		}
	}

	@Test
	fun `Handles dispatched commands by type`() {
		val msgBus = SimpleMessageBus()

		val handler = DummyWorkflow()
		msgBus.registerWorkflow(handler)

		assertThrows<NoHandlersInWorkflowException> {
			msgBus.send(SecondDummyCommand("::test::"))

			assertThat(handler.lastCommand, Is(nullValue()))
		}
	}

	@Test
	fun `Handles dispatch of events through interceptors`() {
		val msgBus = SimpleMessageBus()
		val callLog = mutableListOf<String>()

		msgBus.registerWorkflow(object : Workflow {
			@EventHandler
			fun handle(event: DummyEvent) {
				callLog.add("called handler")
			}
		})

		msgBus.registerInterceptor(object : Interceptor {
			fun intercept(chain: Interceptor) {
				callLog.add("called before")
			}

			override fun intercept(chain: Interceptor.Chain) {
				callLog.add("called before")
				chain.proceed(chain.event())
				callLog.add("called after")
			}
		})

		val event = DummyEvent(UUID.randomUUID())
		msgBus.publish(event)

		assertThat(
			callLog, Is(
				equalTo(
					listOf(
						"called before",
						"called handler",
						"called after"
					)
				)
			)
		)
	}

	@Test
	fun `Handles only interceptors being set`() {
		val msgBus = SimpleMessageBus()
		val callLog = mutableListOf<String>()

		msgBus.registerInterceptor(object : Interceptor {
			override fun intercept(chain: Interceptor.Chain) {
				callLog.add("called before")
				chain.proceed(chain.event())
				callLog.add("called after")
			}
		})

		val event = DummyEvent(UUID.randomUUID())
		msgBus.publish(event)

		assertThat(
			callLog, Is(
				equalTo(
					listOf(
						"called before",
						"called after"
					)
				)
			)
		)
	}


	class DummyWorkflow : Workflow {
		var lastCommand: DummyCommand? = null
		var lastEvent: DummyEvent? = null
		
		@CommandHandler
		fun handle(command: DummyCommand) : CommandResponse {
			lastCommand = command
			return CommandResponse(StatusCode.OK)
		}

		@EventHandler
		fun handle(event: DummyEvent) {
			lastEvent = event
		}
	}
	
	class SecondDummyWorkflow: Workflow {
		var lastEvent: DummyEvent? = null
		
		@EventHandler
		fun handle(event: DummyEvent) {
			lastEvent = event
		}
	}
	class DummyCommand(val name: String) : Command
	class SecondDummyCommand(val name: String) : Command
	class DummyEvent(val name: UUID) : Event
}
