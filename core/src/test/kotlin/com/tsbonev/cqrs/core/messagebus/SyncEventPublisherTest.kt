package com.tsbonev.cqrs.core.messagebus

import com.tsbonev.cqrs.core.PublishErrorException
import com.tsbonev.cqrs.core.TestMessageFormat
import com.tsbonev.cqrs.core.eventstore.CreationContext
import com.tsbonev.cqrs.core.eventstore.EventWithContext
import com.tsbonev.cqrs.core.eventstore.User
import com.tsbonev.cqrs.core.helpers.InMemoryMessageBus
import com.tsbonev.cqrs.core.snapshot.MessageFormat
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers
import org.jmock.AbstractExpectations
import org.jmock.Expectations
import org.jmock.junit5.JUnit5Mockery
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import org.junit.jupiter.api.fail
import java.time.Instant
import org.hamcrest.CoreMatchers.`is` as Is


class SyncEventPublisherTest {

	@RegisterExtension
	@JvmField
	val context = JUnit5Mockery()

	private val mockedMessageBus = context.mock(MessageBus::class.java)

	@Test
	fun `Handles events`() {
		val instant = Instant.now()
		val messageFormat = TestMessageFormat(MyEvent::class.java) as MessageFormat<Any>
		val messageBus = InMemoryMessageBus()
		val syncEventPublisher = SyncEventPublisher(messageBus, messageFormat)
		val firstEvent = MyEvent("Foo")
		val secondEvent = MyEvent("Bar")
		syncEventPublisher.publish(
			listOf(
				EventWithContext(
					messageFormat.format(firstEvent), firstEvent.javaClass.simpleName, 0L, CreationContext(
						User("::id::"), instant.toEpochMilli()
					)
				),
				EventWithContext(
					messageFormat.format(secondEvent), secondEvent.javaClass.simpleName, 0L, CreationContext(
						User("::id::"), instant.toEpochMilli()
					)
				)
			)
		)

		assertThat(messageBus.handledEvents.toList(), Matchers.containsInAnyOrder(firstEvent, secondEvent))
	}

	@Test
	fun `Handles exceptions`() {
		val instant = Instant.now()
		val messageFormat = TestMessageFormat(MyEvent::class.java) as MessageFormat<Any>
		val syncEventPublisher = SyncEventPublisher(mockedMessageBus, messageFormat)
		val firstEvent = MyEvent("Foo")

		context.checking(object : Expectations() {
			init {
				oneOf(mockedMessageBus).publish(firstEvent)
				will(AbstractExpectations.throwException(MyException("Some message!")))
			}
		})

		try {
			syncEventPublisher.publish(
				listOf(
					EventWithContext(
						messageFormat.format(firstEvent), firstEvent.javaClass.simpleName, 0L, CreationContext(
							User("::id::"), instant.toEpochMilli()
						)
					)
				)
			)
			fail("Exception was not thrown")
		} catch (e: PublishErrorException) {
			assertThat(e.reason, Is(Matchers.equalTo(MyException("Some message!") as Exception)))
		}
	}
}

data class MyException(override val message: String) : Exception(message)

data class MyEvent(val foo: String) : Event