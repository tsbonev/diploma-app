package com.tsbonev.cqrs.backend.adapter.mysql

import com.tsbonev.cqrs.core.eventstore.AggregateIdentity
import com.tsbonev.cqrs.core.eventstore.CreationContext
import com.tsbonev.cqrs.core.eventstore.EventSourcedAggregate
import com.tsbonev.cqrs.core.eventstore.EventStore
import com.tsbonev.cqrs.core.eventstore.EventWithContext
import com.tsbonev.cqrs.core.eventstore.Events
import com.tsbonev.cqrs.core.eventstore.SaveEventsResponse
import com.tsbonev.cqrs.core.eventstore.SaveOptions
import com.tsbonev.cqrs.core.messagebus.Event
import org.hamcrest.MatcherAssert.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.context.annotation.ComponentScan
import org.hamcrest.CoreMatchers.`is` as Is

@ComponentScan(basePackageClasses = [MysqlEventStore::class])
@DataJpaTest
class MysqlEventStoreTest constructor(@Autowired val repo: EventStore) {

	@Test
	fun `Saves events`() {
		val response = repo.saveEvents(
			AggregateIdentity("::aggregateId::", "TestAggregate", 1L),
			Events(
				"::aggregateId::", 1L, listOf(
					EventWithContext(StubEvent().toString().toByteArray(), "StubEvent", 0L, CreationContext()),
					EventWithContext(StubEvent().toString().toByteArray(), "StubEvent", 1L, CreationContext())
				)
			),
			SaveOptions()
		) as SaveEventsResponse.Success

		assertThat(
			response, Is(
				SaveEventsResponse.Success(
					EventSourcedAggregate(
						AggregateIdentity("::aggregateId::", "TestAggregate", 1L),
						Events(
							"::aggregateId::", 1L, listOf(
								EventWithContext(
									StubEvent().toString().toByteArray(),
									"StubEvent",
									0L,
									CreationContext()
								),
								EventWithContext(
									StubEvent().toString().toByteArray(),
									"StubEvent",
									1L,
									CreationContext()
								)
							)
						),
						null
					)
				)
			)
		)
	}

	@Test
	fun `Get events of single aggregate`() {
		repo.saveEvents(
			AggregateIdentity("::aggregateId::", "TestAggregate", 1L),
			Events(
				"::aggregateId::", 1L, listOf(
					EventWithContext(StubEvent().toString().toByteArray(), "StubEvent", 0L, CreationContext()),
					EventWithContext(StubEvent().toString().toByteArray(), "StubEvent", 1L, CreationContext())
				)
			),
			SaveOptions()
		) as SaveEventsResponse.Success

		val response = repo.getEvents("::aggregateId::")

		assertThat(
			response, Is(
					EventSourcedAggregate(
						AggregateIdentity("::aggregateId::", "TestAggregate", 1L),
						Events(
							"::aggregateId::", 1L, listOf(
								EventWithContext(
									StubEvent().toString().toByteArray(),
									"StubEvent",
									0L,
									CreationContext()
								),
								EventWithContext(
									StubEvent().toString().toByteArray(),
									"StubEvent",
									1L,
									CreationContext()
								)
							)
						),
						null
					)
				)
			)
	}

	data class StubEvent(val stubData: String = "::stub::") : Event
}