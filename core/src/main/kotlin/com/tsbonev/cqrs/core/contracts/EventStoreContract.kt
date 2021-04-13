package com.tsbonev.cqrs.core.contracts

import com.tsbonev.cqrs.core.BinaryPayload
import com.tsbonev.cqrs.core.eventstore.ConcreteAggregate
import com.tsbonev.cqrs.core.eventstore.CreateSnapshotRequest
import com.tsbonev.cqrs.core.eventstore.EventPayload
import com.tsbonev.cqrs.core.eventstore.EventStore
import com.tsbonev.cqrs.core.eventstore.GetAllEventsRequest
import com.tsbonev.cqrs.core.eventstore.GetAllEventsResponse
import com.tsbonev.cqrs.core.eventstore.GetEventsFromStreamsRequest
import com.tsbonev.cqrs.core.eventstore.GetEventsResponse
import com.tsbonev.cqrs.core.eventstore.Position
import com.tsbonev.cqrs.core.eventstore.ReadDirection
import com.tsbonev.cqrs.core.eventstore.RevertEventsResponse
import com.tsbonev.cqrs.core.eventstore.SaveEventsRequest
import com.tsbonev.cqrs.core.eventstore.SaveEventsResponse
import com.tsbonev.cqrs.core.eventstore.SaveOptions
import com.tsbonev.cqrs.core.snapshot.Snapshot
import org.hamcrest.CoreMatchers
import org.hamcrest.CoreMatchers.`is`
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import java.util.UUID

abstract class EventStoreContract {

	private lateinit var eventStore: EventStore

	@Before
	fun setUp() {
		eventStore = createEventStore()
	}

	@Test
	fun getEventsThatAreStored() {
		eventStore.saveEvents(
			SaveEventsRequest(
				"tenant1", "Invoice_aggregate1", "Invoice", listOf(EventPayload("aggregate1", "::kind::", 1L, "::user 1::", BinaryPayload("::data::")))
			)
		) as SaveEventsResponse.Success

		val response = eventStore.getEventsFromStreams(GetEventsFromStreamsRequest("tenant1", "Invoice_aggregate1"))

		when (response) {
			is GetEventsResponse.Success -> {
				Assert.assertThat(
					response, `is`(
						CoreMatchers.equalTo(
							(
									GetEventsResponse.Success(
										listOf(
											ConcreteAggregate(
												"Invoice",
												null,
												1,
												listOf(
													EventPayload(
														"aggregate1",
														"::kind::",
														1L,
														"::user 1::",
														BinaryPayload("::data::")
													)
												)
											)
										)
									)
									)
						)
					)
				)

			}
			else -> Assert.fail("got unknown response when fetching stored events")
		}
	}

	@Test
	fun allSavedAggregateEventsAreReturned() {
		eventStore.saveEvents(SaveEventsRequest("tenant1", "invoicing", "Invoice",
		                                        listOf(EventPayload("aggregate1", "::kind1::", 1L, "::user 1::", BinaryPayload("::data::"))))
		)

		val response = eventStore.saveEvents(SaveEventsRequest("tenant1", "invoicing", "Invoice",
		                                                       listOf(EventPayload("aggregate1", "::kind2::", 1L, "::user 1::", BinaryPayload("::data2::")))),
		                                     SaveOptions(version = 1)
		) as SaveEventsResponse.Success

		Assert.assertThat(
			response.aggregate, `is`(
				CoreMatchers.equalTo(
					ConcreteAggregate(
						"Invoice", null, 2L, listOf(
							EventPayload("aggregate1", "::kind1::", 1L, "::user 1::", BinaryPayload("::data::")),
							EventPayload("aggregate1", "::kind2::", 1L, "::user 1::", BinaryPayload("::data2::"))
						)
					)
				)
			)
		)
	}


	@Test
	fun multipleEvents() {
		eventStore.saveEvents(SaveEventsRequest("tenant1", "Order_aggregate1", "Order", listOf(
			EventPayload("aggregate1", "::kind1::", 1L, "::user 1::", BinaryPayload("event1-data")),
			EventPayload("aggregate1", "::kind2::", 2L, "::user 2::", BinaryPayload("event2-data"))
		))) as SaveEventsResponse.Success

		val response = eventStore.getEventsFromStreams(GetEventsFromStreamsRequest("tenant1", "Order_aggregate1"))

		when (response) {
			is GetEventsResponse.Success -> {
				Assert.assertThat(
					response, `is`(
						CoreMatchers.equalTo(
							(
									GetEventsResponse.Success(
										listOf(
											ConcreteAggregate(
												"Order",
												null,
												2,
												listOf(
													EventPayload(
														"aggregate1",
														"::kind1::",
														1L,
														"::user 1::",
														BinaryPayload("event1-data")
													),
													EventPayload(
														"aggregate1",
														"::kind2::",
														2L,
														"::user 2::",
														BinaryPayload("event2-data")
													)
												)
											)
										)

									))
						)
					)
				)
			}
			else -> Assert.fail("got unknown response when fetching stored events")
		}
	}

	@Test
	fun getMultipleAggregates() {
		eventStore.saveEvents(SaveEventsRequest("tenant1", "Invoice_aggregate1", "Invoice",

		                                        listOf(EventPayload("aggregate1", "::kind::", 1L, "::user 1::", BinaryPayload("::data::")))
		)) as SaveEventsResponse.Success

		eventStore.saveEvents(SaveEventsRequest("tenant1", "Invoice_aggregate2", "Invoice",
		                                        listOf(EventPayload("aggregate2", "::kind::", 1L, "::user 1::", BinaryPayload("::data::")))
		)) as SaveEventsResponse.Success

		val response = eventStore.getEventsFromStreams(GetEventsFromStreamsRequest("tenant1", listOf("Invoice_aggregate1", "Invoice_aggregate2")))

		when (response) {
			is GetEventsResponse.Success -> {
				Assert.assertThat(
					response.aggregates, CoreMatchers.`is`(
						CoreMatchers.hasItems(
							ConcreteAggregate(
								"Invoice",
								null,
								1,
								listOf(
									EventPayload("aggregate1", "::kind::", 1L, "::user 1::", BinaryPayload("::data::"))
								)
							),
							ConcreteAggregate(
								"Invoice",
								null,
								1,
								listOf(
									EventPayload("aggregate2", "::kind::", 1L, "::user 1::", BinaryPayload("::data::"))
								)
							)
						)
					)
				)

			}
			else -> Assert.fail("got unknown response when fetching stored events")
		}
	}

	@Test
	fun getMultipleAggregatesButNoneMatched() {
		val response = eventStore.getEventsFromStreams(GetEventsFromStreamsRequest("tenant1", listOf("Order_id1", "Order_id2")))

		when (response) {
			is GetEventsResponse.Success -> {
				Assert.assertThat(
					response, `is`(
						CoreMatchers.equalTo(
							(
									GetEventsResponse.Success(
										listOf()
									)
									)
						)
					)
				)

			}
			else -> Assert.fail("got unknown response when fetching stored events")
		}
	}

	@Test
	fun detectEventCollisions() {
		val aggregateId = UUID.randomUUID().toString()

		eventStore.saveEvents(SaveEventsRequest("tenant1", "Order_$aggregateId", "Order", listOf(EventPayload(aggregateId, "::kind::", 1L, "::user 1::", BinaryPayload("::data::")))), SaveOptions(version = 0))

		val saveResult = eventStore.saveEvents(SaveEventsRequest(
			"tenant1",
			"Order_$aggregateId",
			"Order",
			listOf(EventPayload(aggregateId, "::kind::", 1L, "::user 1::", BinaryPayload("::data::")))),
		                                       SaveOptions(version = 0)
		)

		when (saveResult) {
			is SaveEventsResponse.EventCollision -> {
				Assert.assertThat(saveResult.expectedVersion, CoreMatchers.`is`(CoreMatchers.equalTo(1L)))
			}
			else -> Assert.fail("got un-expected save result: $saveResult")
		}
	}

	@Test
	fun revertSavedEvents() {
		val aggregateId = UUID.randomUUID().toString()

		eventStore.saveEvents(SaveEventsRequest("tenant1", "Order_$aggregateId", "Order", listOf(
			EventPayload(aggregateId, "::kind 1::", 1L, "::user1::", BinaryPayload("data0")),
			EventPayload(aggregateId, "::kind 2::", 2L, "::user1::", BinaryPayload("data1")),
			EventPayload(aggregateId, "::kind 3::", 3L, "::user1::", BinaryPayload("data2")),
			EventPayload(aggregateId, "::kind 4::", 4L, "::user2::", BinaryPayload("data3")),
			EventPayload(aggregateId, "::kind 5::", 5L, "::user2::", BinaryPayload("data4"))
		)), SaveOptions(version = 0))

		val response = eventStore.revertEvents("tenant1", "Order_$aggregateId", 2)
		when (response) {
			is RevertEventsResponse.Success -> {
				val resp = eventStore.getEventsFromStreams(GetEventsFromStreamsRequest("tenant1", "Order_$aggregateId")) as GetEventsResponse.Success
				Assert.assertThat(
					resp, `is`(
						CoreMatchers.equalTo(
							GetEventsResponse.Success(
								listOf(
									ConcreteAggregate(
										"Order",
										null,
										3,
										listOf(
											EventPayload(aggregateId, "::kind 1::", 1L, "::user1::", BinaryPayload("data0")),
											EventPayload(aggregateId, "::kind 2::", 2L, "::user1::", BinaryPayload("data1")),
											EventPayload(aggregateId, "::kind 3::", 3L, "::user1::", BinaryPayload("data2"))
										)
									)
								)
							)
						)
					)
				)
			}
			else -> Assert.fail("got un-expected response '$response' when reverting saved events")
		}
	}

	@Test(expected = IllegalArgumentException::class)
	fun revertZeroEventsIsNotAllowed() {
		eventStore.revertEvents("tenant1", "Task_1", 0)
	}

	@Test
	fun revertingMoreThenTheAvailableEvents() {
		val aggregateId = UUID.randomUUID().toString()

		eventStore.saveEvents(SaveEventsRequest("tenant1", "A1_$aggregateId", "A1", listOf(
			EventPayload(aggregateId, "::kind 1::", 1L, "::user id::", BinaryPayload("data0")),
			EventPayload(aggregateId, "::kind 2::", 2L, "::user id::", BinaryPayload("data1"))
		)), SaveOptions(version = 0))

		val response = eventStore.revertEvents("tenant1", "A1_$aggregateId", 5)
		when (response) {
			is RevertEventsResponse.InsufficientEventsError -> {
			}
			else -> Assert.fail("got un-expected response '$response' when reverting more then available")
		}
	}

	@Test
	fun revertFromUnknownConcreteAggregate() {
		eventStore.revertEvents("Type", "::unknown aggregate::", 1) as RevertEventsResponse.AggregateNotFound
	}

	@Test
	fun saveStringWithTooBigSize() {
		val tooBigStringData = "aaaaa".repeat(150000)
		eventStore.saveEvents(
			SaveEventsRequest(
				"tenant1", "Invoice_aggregate1", "Invoice",
				listOf(EventPayload("aggregate1", "::kind::", 1L, "::user 1::", BinaryPayload(tooBigStringData)))
			)
		) as SaveEventsResponse.Success

		val response = eventStore.getEventsFromStreams(GetEventsFromStreamsRequest("tenant1", "Invoice_aggregate1"))

		when (response) {
			is GetEventsResponse.Success -> {
				Assert.assertThat(
					response, `is`(
						CoreMatchers.equalTo(
							(
									GetEventsResponse.Success(
										listOf(
											ConcreteAggregate(
												"Invoice",
												null,
												1,
												listOf(
													EventPayload(
														"aggregate1",
														"::kind::",
														1L,
														"::user 1::",
														BinaryPayload(tooBigStringData)
													)
												)
											)
										)
									)
									)
						)
					)
				)

			}
			else -> Assert.fail("got unknown response when fetching stored events")
		}
	}

	@Test
	fun saveEventExceedsEntityLimitationsAndReturnsCurrentEvents() {
		eventStore.saveEvents(SaveEventsRequest("tenant1", "Invoice_aggregate1", "Invoice",
		                                        listOf(EventPayload("aggregate1", "::kind::", 1L, "::user 1::", BinaryPayload("::data::"))))
		)

		val tooBigStringData = "aaaaaaaa".repeat(150000)

		val eventLimitReachedResponse = eventStore.saveEvents(SaveEventsRequest("tenant1", "Invoice_aggregate1", "Invoice",
		                                                                        listOf(EventPayload("aggregate1", "::kind::", 1L, "::user 1::", BinaryPayload(tooBigStringData)))),
		                                                      SaveOptions(version = 1)
		) as SaveEventsResponse.SnapshotRequired

		Assert.assertThat(
			eventLimitReachedResponse.currentEvents,
			`is`(
				CoreMatchers.equalTo(
					listOf(
						EventPayload(
							"aggregate1",
							"::kind::",
							1L,
							"::user 1::",
							BinaryPayload("::data::")
						)
					)
				)
			)
		)
	}

	@Test
	fun getAllEventsAndSingleIsAvailable() {
		eventStore.saveEvents(SaveEventsRequest("tenant1", "Invoice_aggregate1", "Invoice",
		                                        listOf(EventPayload("aggregate1", "::kind::", 1L, "::user 1::", BinaryPayload("::data::"))))
		)

		val response = eventStore.getAllEvents(GetAllEventsRequest(null, 5, ReadDirection.FORWARD)) as GetAllEventsResponse.Success

		Assert.assertThat(response.events.size, CoreMatchers.`is`(CoreMatchers.equalTo(1)))
		Assert.assertThat(
			response.events[0].payload,
			`is`(
				CoreMatchers.equalTo(
					EventPayload(
						"aggregate1",
						"::kind::",
						1L,
						"::user 1::",
						BinaryPayload("::data::")
					)
				)
			)
		)
		Assert.assertThat(response.nextPosition!!.value, `is`(CoreMatchers.equalTo(response.events[0].position.value)))
	}

	@Test
	fun getAllEventsAndMultipleAreAvailable() {
		eventStore.saveEvents(
			SaveEventsRequest(
				"tenant1", "Invoice_aggregate1", "Invoice",
				listOf(
					EventPayload("aggregate1", "::kind1::", 1L, "::user 1::", BinaryPayload("::data::")),
					EventPayload("aggregate1", "::kind2::", 1L, "::user 1::", BinaryPayload("::data::")),
					EventPayload("aggregate1", "::kind3::", 1L, "::user 1::", BinaryPayload("::data::"))
				)
			),
			saveOptions = SaveOptions(aggregateId = "aggregate1")
		)

		val response = eventStore.getAllEvents(GetAllEventsRequest(null, 5, ReadDirection.FORWARD)) as GetAllEventsResponse.Success

		Assert.assertThat(response.events.size, CoreMatchers.`is`(CoreMatchers.equalTo(3)))
		Assert.assertThat(
			response.events[0].payload,
			`is`(
				CoreMatchers.equalTo(
					EventPayload(
						"aggregate1",
						"::kind1::",
						1L,
						"::user 1::",
						BinaryPayload("::data::")
					)
				)
			)
		)
		Assert.assertThat(
			response.events[1].payload,
			`is`(
				CoreMatchers.equalTo(
					EventPayload(
						"aggregate1",
						"::kind2::",
						1L,
						"::user 1::",
						BinaryPayload("::data::")
					)
				)
			)
		)
		Assert.assertThat(
			response.events[2].payload,
			`is`(
				CoreMatchers.equalTo(
					EventPayload(
						"aggregate1",
						"::kind3::",
						1L,
						"::user 1::",
						BinaryPayload("::data::")
					)
				)
			)
		)
		Assert.assertThat(response.nextPosition!!.value, `is`(CoreMatchers.equalTo(response.events[2].position.value)))
	}

	@Test
	fun getAllEventsOfMultipleAggregates() {
		eventStore.saveEvents(SaveEventsRequest("tenant1", "Invoice_aggregate1", "Invoice",
		                                        listOf(EventPayload("aggregate1", "::kind 1::", 1L, "::user 1::", BinaryPayload("::data::")))),
		                      saveOptions = SaveOptions(aggregateId = "aggregate1")
		)
		eventStore.saveEvents(SaveEventsRequest("tenant1", "Invoice_aggregate2", "Invoice",
		                                        listOf(EventPayload("aggregate2", "::kind 2::", 1L, "::user 1::", BinaryPayload("::data::")))),
		                      saveOptions = SaveOptions(aggregateId = "aggregate2")
		)

		val response = eventStore.getAllEvents(GetAllEventsRequest(null, 5, ReadDirection.FORWARD)) as GetAllEventsResponse.Success

		Assert.assertThat(response.events.size, CoreMatchers.`is`(CoreMatchers.equalTo(2)))
		Assert.assertThat(
			response.events[0].payload,
			`is`(
				CoreMatchers.equalTo(
					EventPayload(
						"aggregate1",
						"::kind 1::",
						1L,
						"::user 1::",
						BinaryPayload("::data::")
					)
				)
			)
		)
		Assert.assertThat(
			response.events[1].payload,
			`is`(
				CoreMatchers.equalTo(
					EventPayload(
						"aggregate2",
						"::kind 2::",
						1L,
						"::user 1::",
						BinaryPayload("::data::")
					)
				)
			)
		)
	}

	@Test
	fun getAllEventsFilteredByStream() {
		eventStore.saveEvents(SaveEventsRequest("tenant1", "Order_aggregate1", "Order",
		                                        listOf(EventPayload("aggregate1", "::kind 1::", 1L, "::user 1::", BinaryPayload("::data::")))),
		                      saveOptions = SaveOptions(aggregateId = "aggregate1")
		)

		eventStore.saveEvents(SaveEventsRequest("tenant1", "Invoice_aggregate2", "Invoice",
		                                        listOf(EventPayload("aggregate2", "::kind 2::", 1L, "::user 1::", BinaryPayload("::data::")))),
		                      saveOptions = SaveOptions(aggregateId = "aggregate2")
		)

		eventStore.saveEvents(SaveEventsRequest(
			"tenant1",
			"Shipment_aggregate3",
			"Shipment",
			listOf(EventPayload("aggregate3", "::kind 3::", 1L, "::user 1::", BinaryPayload("::data::")))),
		                      saveOptions = SaveOptions(aggregateId = "aggregate3")
		)

		val response = eventStore.getAllEvents(GetAllEventsRequest(null, 5, ReadDirection.FORWARD, listOf("Order_aggregate1", "Invoice_aggregate2"))) as GetAllEventsResponse.Success

		Assert.assertThat(response.events.size, CoreMatchers.`is`(CoreMatchers.equalTo(2)))
		Assert.assertThat(
			response.events[0].payload,
			`is`(
				CoreMatchers.equalTo(
					EventPayload(
						"aggregate1",
						"::kind 1::",
						1L,
						"::user 1::",
						BinaryPayload("::data::")
					)
				)
			)
		)
		Assert.assertThat(
			response.events[1].payload,
			`is`(
				CoreMatchers.equalTo(
					EventPayload(
						"aggregate2",
						"::kind 2::",
						1L,
						"::user 1::",
						BinaryPayload("::data::")
					)
				)
			)
		)
	}

	@Test
	fun onlyMaxCountIsRetrieved() {
		eventStore.saveEvents(SaveEventsRequest("tenant1", "Invoice_aggregate1", "Invoice",
		                                        listOf(
			                                        EventPayload("aggregate1", "::kind1::", 1L, "::user 1::", BinaryPayload("::data::")),
			                                        EventPayload("aggregate1", "::kind2::", 1L, "::user 1::", BinaryPayload("::data::")),
			                                        EventPayload("aggregate1", "::kind3::", 1L, "::user 1::", BinaryPayload("::data::"))
		                                        )),
		                      saveOptions = SaveOptions(aggregateId = "aggregate1")
		)

		val response = eventStore.getAllEvents(GetAllEventsRequest(null, 2, ReadDirection.FORWARD)) as GetAllEventsResponse.Success
		Assert.assertThat(response.events.size, CoreMatchers.`is`(CoreMatchers.equalTo(2)))
	}

	@Test
	fun readFromRequestedPosition() {
		val saveResponse = eventStore.saveEvents(SaveEventsRequest("tenant1", "billing", "Invoice",
		                                                           listOf(
			                                                           EventPayload("aggregate1", "::kind 1::", 1L, "::user 1::", BinaryPayload("::data::")),
			                                                           EventPayload("aggregate1", "::kind 2::", 1L, "::user 1::", BinaryPayload("::data::")),
			                                                           EventPayload("aggregate1", "::kind 3::", 1L, "::user 1::", BinaryPayload("::data::"))
		                                                           )),
		                                         saveOptions = SaveOptions(aggregateId = "aggregate1")
		) as SaveEventsResponse.Success

		val response = eventStore.getAllEvents(GetAllEventsRequest(Position(saveResponse.sequenceIds[0]), 3, ReadDirection.FORWARD)) as GetAllEventsResponse.Success

		Assert.assertThat(response.events.size, CoreMatchers.`is`(CoreMatchers.equalTo(2)))
		Assert.assertThat(
			response.events[0].payload,
			`is`(
				CoreMatchers.equalTo(
					EventPayload(
						"aggregate1",
						"::kind 2::",
						1L,
						"::user 1::",
						BinaryPayload("::data::")
					)
				)
			)
		)
		Assert.assertThat(
			response.events[1].payload,
			`is`(
				CoreMatchers.equalTo(
					EventPayload(
						"aggregate1",
						"::kind 3::",
						1L,
						"::user 1::",
						BinaryPayload("::data::")
					)
				)
			)
		)
	}

	@Test
	fun readFromRequestedPositionWhenMultipleAggregates() {
		eventStore.saveEvents(SaveEventsRequest("tenant1", "Invoice_aggregate1", "Invoice",
		                                        listOf(EventPayload("aggregate1", "::kind 1::", 1L, "::user 1::", BinaryPayload("::data::")))),
		                      saveOptions = SaveOptions(aggregateId = "aggregate1")
		) as SaveEventsResponse.Success

		val saveResponse = eventStore.saveEvents(SaveEventsRequest("tenant1", "Invoice_aggregate2", "Invoice",
		                                                           listOf(EventPayload("aggregate2", "::kind 1::", 1L, "::user 1::", BinaryPayload("::data::"))))
		) as SaveEventsResponse.Success

		val response = eventStore.getAllEvents(GetAllEventsRequest(Position(saveResponse.sequenceIds[0] - 1), 3, ReadDirection.FORWARD)) as GetAllEventsResponse.Success

		Assert.assertThat(response.events.size, CoreMatchers.`is`(CoreMatchers.equalTo(1)))
		Assert.assertThat(
			response.events[0].payload,
			`is`(
				CoreMatchers.equalTo(
					EventPayload(
						"aggregate2",
						"::kind 1::",
						1L,
						"::user 1::",
						BinaryPayload("::data::")
					)
				)
			)
		)
	}

	@Test
	fun readFromBack() {
		val saveResponse = eventStore.saveEvents(SaveEventsRequest("tenant1", "invoicing", "Invoice",
		                                                           listOf(
			                                                           EventPayload("aggregate1", "::kind 1::", 1L, "::user 1::", BinaryPayload("::data::")),
			                                                           EventPayload("aggregate1", "::kind 2::", 1L, "::user 1::", BinaryPayload("::data::")),
			                                                           EventPayload("aggregate1", "::kind 3::", 1L, "::user 1::", BinaryPayload("::data::"))
		                                                           ))
		) as SaveEventsResponse.Success

		val response = eventStore.getAllEvents(GetAllEventsRequest(Position(saveResponse.sequenceIds[2] + 1), 3, ReadDirection.BACKWARD)) as GetAllEventsResponse.Success

		Assert.assertThat(response.events.size, CoreMatchers.`is`(CoreMatchers.equalTo(3)))
		Assert.assertThat(
			response.events[0].payload,
			`is`(
				CoreMatchers.equalTo(
					EventPayload(
						"aggregate1",
						"::kind 3::",
						1L,
						"::user 1::",
						BinaryPayload("::data::")
					)
				)
			)
		)
		Assert.assertThat(
			response.events[1].payload,
			`is`(
				CoreMatchers.equalTo(
					EventPayload(
						"aggregate1",
						"::kind 2::",
						1L,
						"::user 1::",
						BinaryPayload("::data::")
					)
				)
			)
		)
		Assert.assertThat(
			response.events[2].payload,
			`is`(
				CoreMatchers.equalTo(
					EventPayload(
						"aggregate1",
						"::kind 1::",
						1L,
						"::user 1::",
						BinaryPayload("::data::")
					)
				)
			)
		)
	}

	@Test
	fun returningManyEventsOnLimitReached() {
		eventStore.saveEvents(SaveEventsRequest("tenant1", "invoicing", "Invoice",
		                                        listOf(EventPayload("invoicing", "::kind::", 1L, "::user 1::", BinaryPayload("::data::"))))
		)

		eventStore.saveEvents(SaveEventsRequest("tenant1", "invoicing", "Invoice",
		                                        listOf(EventPayload("invoicing", "::kind::", 1L, "::user 1::", BinaryPayload("::data2::")))),
		                      SaveOptions(version = 1)
		)

		val tooBigStringData = "aaaaaaaa".repeat(150000)

		val eventLimitReachedResponse = eventStore.saveEvents(SaveEventsRequest("tenant1", "invoicing", "Invoice",
		                                                                        listOf(EventPayload("invoicing", "::kind::", 1L, "::user 1::", BinaryPayload(tooBigStringData)))),
		                                                      SaveOptions(version = 2)
		) as SaveEventsResponse.SnapshotRequired

		Assert.assertThat(
			eventLimitReachedResponse.currentEvents, `is`(
				CoreMatchers.equalTo(
					listOf(
						EventPayload("invoicing", "::kind::", 1L, "::user 1::", BinaryPayload("::data::")),
						EventPayload("invoicing", "::kind::", 1L, "::user 1::", BinaryPayload("::data2::"))
					)
				)
			)
		)
	}

	@Test
	fun onEventLimitReachSnapshotIsReturned() {
		eventStore.saveEvents(
			SaveEventsRequest(
				"tenant1", "Invoice_aggregate1", "Invoice",
				listOf(EventPayload("aggregate1", "::kind::", 1L, "::user 1::", BinaryPayload("::data0::")))
			),
			SaveOptions(version = 0, createSnapshotRequest = CreateSnapshotRequest(true, Snapshot(0, BinaryPayload("::snapshotData::"))))
		)

		eventStore.saveEvents(
			SaveEventsRequest(
				"tenant1", "Invoice_aggregate1", "Invoice",
				listOf(EventPayload("aggregate1", "::kind::", 1L, "::user 1::", BinaryPayload("::data::")))
			),
			SaveOptions(version = 1, createSnapshotRequest = CreateSnapshotRequest(true, Snapshot(1, BinaryPayload("::snapshotData::"))))
		)

		val tooBigStringData = "aaaaaaaa".repeat(150000)

		val eventLimitReachedResponse = eventStore.saveEvents(SaveEventsRequest("tenant1", "Invoice_aggregate1", "Invoice",
		                                                                        listOf(EventPayload("aggregate1", "::kind::", 1L, "::user 1::", BinaryPayload(tooBigStringData)))),
		                                                      SaveOptions(version = 2)
		) as SaveEventsResponse.SnapshotRequired

		Assert.assertThat(
			eventLimitReachedResponse.currentEvents,
			`is`(
				CoreMatchers.equalTo(
					listOf(
						EventPayload(
							"aggregate1",
							"::kind::",
							1L,
							"::user 1::",
							BinaryPayload("::data::")
						)
					)
				)
			)
		)
		Assert.assertThat(
			eventLimitReachedResponse.currentSnapshot,
			`is`(CoreMatchers.equalTo(Snapshot(1, BinaryPayload("::snapshotData::"))))
		)
	}

	@Test
	fun requestingSnapshotSave() {
		eventStore.saveEvents(SaveEventsRequest("tenant1", "Invoice_aggregate1", "Invoice",
		                                        listOf(EventPayload("aggregate1", "::kind::", 1L, "::user 1::", BinaryPayload("::data::")))),
		                      SaveOptions(version = 0, topicName = "::topic::", createSnapshotRequest = CreateSnapshotRequest(true, Snapshot(0, BinaryPayload("::snapshotData::"))))
		) as SaveEventsResponse.Success

		val response = eventStore.getEventsFromStreams(GetEventsFromStreamsRequest("tenant1", "Invoice_aggregate1"))
		when (response) {
			is GetEventsResponse.Success -> {
				Assert.assertThat(
					response, `is`(
						CoreMatchers.equalTo(
							(
									GetEventsResponse.Success(
										listOf(
											ConcreteAggregate(
												"Invoice",
												Snapshot(0, BinaryPayload("::snapshotData::")),
												1,
												listOf(
													EventPayload(
														"aggregate1",
														"::kind::",
														1L,
														"::user 1::",
														BinaryPayload("::data::")
													)
												)
											)
										)
									)
									)
						)
					)
				)
			}
			else -> Assert.fail("got unknown response when fetching stored events")
		}
	}

	@Test
	fun saveEventIsReturningSnapshotWhenAvailable() {
		eventStore.saveEvents(SaveEventsRequest("tenant1", "Invoice_aggregate1", "Invoice",
		                                        listOf(EventPayload("aggregate1", "::kind::", 1L, "::user 1::", BinaryPayload("::data::")))),
		                      SaveOptions("::aggregateId::", 1, "::topic::", CreateSnapshotRequest(true, Snapshot(1, BinaryPayload("::snapshotData::"))))
		)

		val saveResponse = eventStore.saveEvents(SaveEventsRequest("tenant1", "Invoice_aggregate1", "Invoice",
		                                                           listOf(EventPayload("aggregate1", "::kind::", 1L, "::user 1::", BinaryPayload("::data2::")))),
		                                         SaveOptions("::aggregateId::", 2L)
		) as SaveEventsResponse.Success

		Assert.assertThat(
			saveResponse.aggregate.snapshot,
			`is`(CoreMatchers.equalTo(Snapshot(1L, BinaryPayload("::snapshotData::"))))
		)
	}

	@Test
	fun saveEventsAfterSnapshotChange() {
		eventStore.saveEvents(SaveEventsRequest("tenant1", "Invoice_aggregate1", "Invoice",
		                                        listOf(EventPayload("aggregate1", "::kind::", 1L, "::user 1::", BinaryPayload("::data::")))),
		                      SaveOptions("::aggregateId::", 1, "::topic::", CreateSnapshotRequest(true, Snapshot(1, BinaryPayload("::snapshotData::"))))
		)

		val success = eventStore.getEventsFromStreams(GetEventsFromStreamsRequest("tenant1", "Invoice_aggregate1")) as GetEventsResponse.Success

		eventStore.saveEvents(SaveEventsRequest("tenant1", "Invoice_aggregate1", "Invoice",
		                                        listOf(EventPayload("aggregate1", "::kind::", 1L, "::user 1::", BinaryPayload("::data2::")))),
		                      SaveOptions("::aggregateId::", success.aggregates[0].version)
		) as SaveEventsResponse.Success

		val response = eventStore.getEventsFromStreams(GetEventsFromStreamsRequest("tenant1", "Invoice_aggregate1"))
		when (response) {
			is GetEventsResponse.Success -> {
				Assert.assertThat(
					response, `is`(
						CoreMatchers.equalTo(
							(
									GetEventsResponse.Success(
										listOf(
											ConcreteAggregate(
												"Invoice",
												Snapshot(1, BinaryPayload("::snapshotData::")),
												3,
												listOf(
													EventPayload(
														"aggregate1",
														"::kind::",
														1L,
														"::user 1::",
														BinaryPayload("::data::")
													),
													EventPayload(
														"aggregate1",
														"::kind::",
														1L,
														"::user 1::",
														BinaryPayload("::data2::")
													)
												)
											)
										)
									)
									)
						)
					)
				)

			}
			else -> Assert.fail("got unknown response when fetching stored events")
		}
	}

	@Test
	fun saveManySnapshots() {
		// save event for first time
		eventStore.saveEvents(SaveEventsRequest("tenant1", "Invoice_aggregate1", "Invoice",
		                                        listOf(EventPayload("aggregate1", "::kind::", 1L, "::user 1::", BinaryPayload("::data::")))),
		                      SaveOptions("::aggregateId::", 0, "::topic::", CreateSnapshotRequest(false))
		)

		//fetch the current aggregate value and provide the current version
		val noSnapshotResponse = eventStore.getEventsFromStreams(GetEventsFromStreamsRequest("tenant1", "Invoice_aggregate1")) as GetEventsResponse.Success
		val noSnapshotVersion = noSnapshotResponse.aggregates[0].version

		eventStore.saveEvents(SaveEventsRequest("tenant1", "Invoice_aggregate1", "Invoice",
		                                        listOf(EventPayload("aggregate1", "::kind::", 1L, "::user 1::", BinaryPayload("::data::")))),
		                      SaveOptions("::aggregateId::", noSnapshotVersion, "::topic::", CreateSnapshotRequest(true, Snapshot(noSnapshotVersion, BinaryPayload("::snapshotData::"))))
		)

		//fetch the current aggregate value and provide the current version
		val firstSnapshotResponse = eventStore.getEventsFromStreams(GetEventsFromStreamsRequest("tenant1", "Invoice_aggregate1")) as GetEventsResponse.Success
		val firstSnapshotVersion = firstSnapshotResponse.aggregates[0].version

		val response = eventStore.saveEvents(SaveEventsRequest("tenant1", "Invoice_aggregate1", "Invoice",
		                                                       listOf(EventPayload("aggregate1", "::kind::", 1L, "::user 1::", BinaryPayload("::data2::")))),
		                                     SaveOptions("::aggregateId::", firstSnapshotVersion, "::topic::", CreateSnapshotRequest(true, Snapshot(firstSnapshotVersion, BinaryPayload("::snapshotData2::"))))
		) as SaveEventsResponse.Success

		val success = eventStore.getEventsFromStreams(GetEventsFromStreamsRequest("tenant1", "Invoice_aggregate1"))

		when (success) {
			is GetEventsResponse.Success -> {
				Assert.assertThat(
					success, `is`(
						CoreMatchers.equalTo(
							(
									GetEventsResponse.Success(
										listOf(
											ConcreteAggregate(
												"Invoice",
												Snapshot(2, BinaryPayload("::snapshotData2::")),
												3,
												listOf(
													EventPayload(
														"aggregate1",
														"::kind::",
														1L,
														"::user 1::",
														BinaryPayload("::data2::")
													)
												)
											)
										)
									)
									)
						)
					)
				)

			}
			else -> Assert.fail("got unknown response when fetching stored events")
		}
	}

	abstract fun createEventStore(): EventStore
}