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
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.hamcrest.CoreMatchers.`is` as Is

@ExtendWith(SpringExtension::class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
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
	fun `Saves one event`() {
		val response = repo.saveEvents(
			AggregateIdentity("::aggregateId::", "TestAggregate", 0L),
			Events(
				"::aggregateId::", 0L, listOf(
					EventWithContext(StubEvent().toString().toByteArray(), "StubEvent", 0L, CreationContext()),
				)
			),
			SaveOptions()
		) as SaveEventsResponse.Success

		assertThat(
			response, Is(
				SaveEventsResponse.Success(
					EventSourcedAggregate(
						AggregateIdentity("::aggregateId::", "TestAggregate", 0L),
						Events(
							"::aggregateId::", 0L, listOf(
								EventWithContext(
									StubEvent().toString().toByteArray(),
									"StubEvent",
									0L,
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
		)

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

	@Test
	fun `Get events of multiple aggregates`() {
		repo.saveEvents(
			AggregateIdentity("::aggregateId_1::", "TestAggregate", 1L),
			Events(
				"::aggregateId::", 1L, listOf(
					EventWithContext(StubEvent().toString().toByteArray(), "StubEvent", 0L, CreationContext()),
					EventWithContext(StubEvent().toString().toByteArray(), "StubEvent", 1L, CreationContext())
				)
			),
			SaveOptions()
		)

		repo.saveEvents(
			AggregateIdentity("::aggregateId_2::", "TestAggregate", 1L),
			Events(
				"::aggregateId::", 1L, listOf(
					EventWithContext(StubEvent().toString().toByteArray(), "StubEvent", 0L, CreationContext()),
				)
			),
			SaveOptions()
		)

		val response = repo.getEvents(listOf("::aggregateId_1::", "::aggregateId_2::"))

		assertThat(
			response, Is(
				listOf(
					EventSourcedAggregate(
						AggregateIdentity("::aggregateId_1::", "TestAggregate", 1L),
						Events(
							"::aggregateId_1::", 1L, listOf(
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
					),
					EventSourcedAggregate(
						AggregateIdentity("::aggregateId_2::", "TestAggregate", 0L),
						Events(
							"::aggregateId_2::", 0L, listOf(
								EventWithContext(
									StubEvent().toString().toByteArray(),
									"StubEvent",
									0L,
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
	fun `Get events of single aggregate after multiple saves`() {
		repo.saveEvents(
			AggregateIdentity("::aggregateId::", "TestAggregate", 1L),
			Events(
				"::aggregateId::", 1L, listOf(
					EventWithContext(StubEvent().toString().toByteArray(), "StubEvent", 0L, CreationContext()),
					EventWithContext(StubEvent().toString().toByteArray(), "StubEvent", 1L, CreationContext())
				)
			),
			SaveOptions()
		)

		repo.saveEvents(
			AggregateIdentity("::aggregateId::", "TestAggregate", 2L),
			Events(
				"::aggregateId::", 2L, listOf(
					EventWithContext(StubEvent().toString().toByteArray(), "StubEvent", 2L, CreationContext()),
				)
			),
			SaveOptions()
		)

		val response = repo.getEvents("::aggregateId::")

		assertThat(
			response, Is(
				EventSourcedAggregate(
					AggregateIdentity("::aggregateId::", "TestAggregate", 2L),
					Events(
						"::aggregateId::", 2L, listOf(
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
							),
							EventWithContext(
								StubEvent().toString().toByteArray(),
								"StubEvent",
								2L,
								CreationContext()
							)
						)
					),
					null
				)
			)
		)
	}

	@Test
	fun `Save results in event collision when missing`() {
		repo.saveEvents(
			AggregateIdentity("::aggregateId::", "TestAggregate", 1L),
			Events(
				"::aggregateId::", 1L, listOf(
					EventWithContext(StubEvent().toString().toByteArray(), "StubEvent", 0L, CreationContext()),
					EventWithContext(StubEvent().toString().toByteArray(), "StubEvent", 1L, CreationContext())
				)
			),
			SaveOptions()
		)

		val response = repo.saveEvents(
			AggregateIdentity("::aggregateId::", "TestAggregate", 4L),
			Events(
				"::aggregateId::", 4L, listOf(
					EventWithContext(StubEvent().toString().toByteArray(), "StubEvent", 4L, CreationContext()),
				)
			),
			SaveOptions()
		) as SaveEventsResponse.EventCollision


		assertThat(response, Is(SaveEventsResponse.EventCollision(2L, 4L)))
	}

	@Test
	fun `Save results in event collision when overlap`() {
		repo.saveEvents(
			AggregateIdentity("::aggregateId::", "TestAggregate", 1L),
			Events(
				"::aggregateId::", 1L, listOf(
					EventWithContext(StubEvent().toString().toByteArray(), "StubEvent", 0L, CreationContext()),
					EventWithContext(StubEvent().toString().toByteArray(), "StubEvent", 1L, CreationContext())
				)
			),
			SaveOptions()
		)

		val response = repo.saveEvents(
			AggregateIdentity("::aggregateId::", "TestAggregate", 1L),
			Events(
				"::aggregateId::", 1L, listOf(
					EventWithContext(StubEvent().toString().toByteArray(), "StubEvent", 1L, CreationContext()),
				)
			),
			SaveOptions()
		) as SaveEventsResponse.EventCollision


		assertThat(response, Is(SaveEventsResponse.EventCollision(2L, 1L)))
	}

	@Test
	fun `Exceeding limit requires snapshot`() {
		val mysqlRepo = repo as MysqlEventStore
		mysqlRepo.setEventLimit(2)

		repo.saveEvents(
			AggregateIdentity("::aggregateId::", "TestAggregate", 0L),
			Events(
				"::aggregateId::", 0L, listOf(
					EventWithContext(StubEvent().toString().toByteArray(), "StubEvent", 0L, CreationContext()),
				)
			),
			SaveOptions()
		) as SaveEventsResponse.Success

		val response = repo.saveEvents(
			AggregateIdentity("::aggregateId::", "TestAggregate", 1L),
			Events(
				"::aggregateId::", 1L, listOf(
					EventWithContext(StubEvent().toString().toByteArray(), "StubEvent", 1L, CreationContext()),
				)
			),
			SaveOptions()
		) as SaveEventsResponse.SnapshotRequired


		assertThat(
			response, Is(
				SaveEventsResponse.SnapshotRequired(
					Events(
						"::aggregateId::", 1L, listOf(
							EventWithContext(StubEvent().toString().toByteArray(), "StubEvent", 1L, CreationContext()),
						)
					),
					null
				)
			)
		)
	}


	data class StubEvent(val stubData: String = "::stub::") : Event
}