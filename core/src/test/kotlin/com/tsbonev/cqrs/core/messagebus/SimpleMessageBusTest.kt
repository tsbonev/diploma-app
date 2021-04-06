package com.tsbonev.cqrs.core.messagebus

import com.tsbonev.cqrs.core.BinaryPayload
import com.tsbonev.cqrs.core.Command
import com.tsbonev.cqrs.core.CommandHandler
import com.tsbonev.cqrs.core.Event
import com.tsbonev.cqrs.core.EventHandler
import com.tsbonev.cqrs.core.EventWithBinaryPayload
import com.tsbonev.cqrs.core.ValidationException
import org.hamcrest.CoreMatchers
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.CoreMatchers.nullValue
import org.junit.Assert
import org.junit.Test
import java.util.UUID
import org.hamcrest.CoreMatchers.`is` as Is
import org.junit.Assert.assertThat

/**
 * @author Tsvetozar Bonev (tsbonev@gmail.com)
 */
class SimpleMessageBusTest {

	@Test
	fun `Handles event with a single handler`() {
		val msgBus = SimpleMessageBus()

		val handler = MyEventHandler()
		msgBus.registerEventHandler(MyEvent::class.java, handler)

		val event = EventWithBinaryPayload(MyEvent(UUID.randomUUID()), BinaryPayload(""))
		msgBus.handle(event)

		assertThat(handler.lastEvent, Is(equalTo(event.event)))
	}

	@Test
	fun `Handles event with multiple handler`() {
		val msgBus = SimpleMessageBus()

		val firstHandler = MyEventHandler()
		val secondHandler = AnotherHandler()

		msgBus.registerEventHandler(MyEvent::class.java, firstHandler)
		msgBus.registerEventHandler(MyEvent::class.java, secondHandler)

		val event = EventWithBinaryPayload(MyEvent(UUID.randomUUID()), BinaryPayload(""))
		msgBus.handle(event)

		assertThat(firstHandler.lastEvent, Is(CoreMatchers.equalTo(event.event)))
		assertThat(secondHandler.lastEvent, Is(CoreMatchers.equalTo(event.event)))

	}

	@Test
	fun `No handlers are set`() {
		val msgBus = SimpleMessageBus()
		msgBus.handle(EventWithBinaryPayload(MyEvent(UUID.randomUUID()), BinaryPayload("")))
	}

	@Test
	fun `Handles command with command handler`() {
		val msgBus = SimpleMessageBus()

		val handler = ChangeCustomerNameHandler()
		msgBus.registerCommandHandler(ChangeCustomerName::class.java, handler)

		val changeCustomerNameAction = ChangeCustomerName("Action")
		msgBus.send(changeCustomerNameAction)

		assertThat(handler.lastCommand, Is(changeCustomerNameAction))
	}

	@Test
	fun `Handles command with command handler and returns a response`() {
		val msgBus = SimpleMessageBus()

		val handler = ChangeCustomerNameHandler()
		msgBus.registerCommandHandler(ChangeCustomerName::class.java, handler)

		val changeCustomerNameAction = ChangeCustomerName("Action")
		val response = msgBus.send(changeCustomerNameAction)

		assertThat(response, Is("OK"))
		assertThat(handler.lastCommand, Is(changeCustomerNameAction))
	}

	@Test(expected = IllegalArgumentException::class)
	fun `No proper handler is set`() {
		val msgBus = SimpleMessageBus()

		val changeCustomerNameAction = ChangeCustomerName("Action")
		msgBus.send(changeCustomerNameAction)
	}

	@Test
	fun `Validate receiving command`() {
		val msgBus = SimpleMessageBus()

		val handler = ChangeCustomerNameHandler()
		msgBus.registerCommandHandler(ChangeCustomerName::class.java, handler, Validation {
			"name" {
				be {
					name.length > 5
				} not "name: must be at least 5 characters long"
			}
		})

		val changeCustomerNameAction = ChangeCustomerName("Jo")
		try {
			msgBus.send(changeCustomerNameAction)
			Assert.fail("validation was not performed during sending of an invalidation action")
		} catch (ex: ValidationException) {
			assertThat(
				ex.errors,
				Is(equalTo(mapOf("name" to listOf("name: must be at least 5 characters long"))))
			)
			assertThat(handler.lastCommand, Is(nullValue()))
		}
	}

	@Test(expected = java.lang.IllegalArgumentException::class)
	fun `Handles dispatched commands by type`() {
		val msgBus = SimpleMessageBus()

		val handler = ChangeCustomerNameHandler()
		msgBus.registerCommandHandler(ChangeCustomerName::class.java, handler)

		msgBus.send(DummyCommand())

		assertThat(handler.lastCommand, Is(nullValue()))
	}

	@Test
	fun `Handles dispatch of events through interceptors`() {
		val msgBus = SimpleMessageBus()
		val callLog = mutableListOf<String>()

		msgBus.registerEventHandler(MyEvent::class.java, object : EventHandler<MyEvent> {
			override fun handle(event: MyEvent) {
				callLog.add("called handler")
			}

		})

		msgBus.registerInterceptor(object : Interceptor {
			override fun intercept(chain: Interceptor.Chain) {
				callLog.add("called before")
				chain.proceed(chain.event())
				callLog.add("called after")
			}
		})

		val event = EventWithBinaryPayload(MyEvent(UUID.randomUUID()), BinaryPayload(""))
		msgBus.handle(event)

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

		val event = EventWithBinaryPayload(MyEvent(UUID.randomUUID()), BinaryPayload(""))
		msgBus.handle(event)

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


	class ChangeCustomerNameHandler : CommandHandler<ChangeCustomerName, String> {

		var lastCommand: ChangeCustomerName? = null

		override fun handle(command: ChangeCustomerName): String {
			lastCommand = command
			return "OK"
		}
	}

	class DummyCommand : Command<String>

	class ChangeCustomerName(val name: String) : Command<String>

	class MyEventHandler : EventHandler<MyEvent> {
		var lastEvent: MyEvent? = null
		override fun handle(event: MyEvent) {
			lastEvent = event
		}
	}

	class AnotherHandler : EventHandler<MyEvent> {
		var lastEvent: MyEvent? = null

		override fun handle(event: MyEvent) {
			lastEvent = event
		}
	}

	class MyEvent(val name: UUID) : Event
}
