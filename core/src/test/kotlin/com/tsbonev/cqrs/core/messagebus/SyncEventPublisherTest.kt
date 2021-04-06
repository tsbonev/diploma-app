package com.tsbonev.cqrs.core.messagebus

import com.tsbonev.cqrs.core.BinaryPayload
import com.tsbonev.cqrs.core.Event
import com.tsbonev.cqrs.core.EventWithBinaryPayload
import com.tsbonev.cqrs.core.PublishErrorException
import com.tsbonev.cqrs.testing.InMemoryMessageBus
import org.hamcrest.Matchers
import org.jmock.AbstractExpectations
import org.jmock.Expectations
import org.jmock.integration.junit4.JUnitRuleMockery
import org.junit.Assert
import org.junit.Rule
import org.junit.Test
import org.hamcrest.CoreMatchers.`is` as Is
import org.junit.Assert.assertThat

/**
 * @author Tsvetozar Bonev (tsbonev@gmail.com)
 */
class SyncEventPublisherTest {

	@Rule
	@JvmField
	val context = JUnitRuleMockery()

	private val mockedMessageBus = context.mock(MessageBus::class.java)

	@Test
	fun `Handles events`() {
		val messageBus = InMemoryMessageBus()
		val syncEventPublisher = SyncEventPublisher(messageBus)
		val firstEvent = EventWithBinaryPayload(MyEvent("Foo"), BinaryPayload("::payload::"))
		val secondEvent = EventWithBinaryPayload(MyEvent("Bar"), BinaryPayload("::otherPayload::"))
		syncEventPublisher.publish(listOf(
			firstEvent,
			secondEvent
		))

		assertThat(messageBus.handledEvents, Matchers.containsInAnyOrder(firstEvent, secondEvent))
	}

	@Test
	fun `Handles exceptions`() {
		val syncEventPublisher = SyncEventPublisher(mockedMessageBus)
		val firstEvent = EventWithBinaryPayload(MyEvent("Foo"), BinaryPayload("::payload::"))

		context.checking(object : Expectations() {
			init {
				oneOf(mockedMessageBus).handle(firstEvent)
				will(AbstractExpectations.throwException(MyException("Some message!")))
			}
		})
		try {
			syncEventPublisher.publish(listOf(firstEvent))
			Assert.fail("Exception was not thrown")
		} catch (e: PublishErrorException) {
			assertThat(e.reason, Is(Matchers.equalTo(MyException("Some message!") as Exception)))
		}
	}
}

data class MyException(override val message: String) : Exception(message)

data class MyEvent(val foo: String) : Event