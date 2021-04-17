package com.tsbonev.cqrs.core.messagebus

import com.tsbonev.cqrs.core.BinaryPayload
import com.tsbonev.cqrs.core.EventWithBinaryPayload
import com.tsbonev.cqrs.core.PublishErrorException
import com.tsbonev.cqrs.core.helpers.InMemoryMessageBus
import org.hamcrest.Matchers
import org.jmock.AbstractExpectations
import org.jmock.Expectations
import org.jmock.junit5.JUnit5Mockery
import org.hamcrest.CoreMatchers.`is` as Is
import org.hamcrest.MatcherAssert.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import org.junit.jupiter.api.fail


class SyncEventPublisherTest {

	@RegisterExtension
	@JvmField
	val context = JUnit5Mockery()

	private val mockedMessageBus = context.mock(MessageBus::class.java)

	@Test
	fun `Handles events`() {
		val messageBus = InMemoryMessageBus()
		val syncEventPublisher = SyncEventPublisher(messageBus)
		val firstEvent = MyEvent("Foo")
		val secondEvent = MyEvent("Bar")
		syncEventPublisher.publish(listOf(
			firstEvent,
			secondEvent
		))

		assertThat(messageBus.handledEvents.toList(), Matchers.containsInAnyOrder(firstEvent, secondEvent))
	}

	@Test
	fun `Handles exceptions`() {
		val syncEventPublisher = SyncEventPublisher(mockedMessageBus)
		val firstEvent = EventWithBinaryPayload(MyEvent("Foo"), BinaryPayload("::payload::"))

		context.checking(object : Expectations() {
			init {
				oneOf(mockedMessageBus).publish(firstEvent)
				will(AbstractExpectations.throwException(MyException("Some message!")))
			}
		})
		try {
			syncEventPublisher.publish(listOf(firstEvent))
			fail("Exception was not thrown")
		} catch (e: PublishErrorException) {
			assertThat(e.reason, Is(Matchers.equalTo(MyException("Some message!") as Exception)))
		}
	}
}

data class MyException(override val message: String) : Exception(message)

data class MyEvent(val foo: String) : Event