package com.tsbonev.cqrs.core.helpers

import com.tsbonev.cqrs.core.eventstore.ConcreteAggregate
import com.tsbonev.cqrs.core.eventstore.EventPayload
import com.tsbonev.cqrs.core.eventstore.EventStore
import com.tsbonev.cqrs.core.eventstore.GetAllEventsRequest
import com.tsbonev.cqrs.core.eventstore.GetAllEventsResponse
import com.tsbonev.cqrs.core.eventstore.GetEventsFromStreamsRequest
import com.tsbonev.cqrs.core.eventstore.GetEventsResponse
import com.tsbonev.cqrs.core.eventstore.IndexedEvent
import com.tsbonev.cqrs.core.eventstore.Position
import com.tsbonev.cqrs.core.eventstore.RevertEventsResponse
import com.tsbonev.cqrs.core.eventstore.SaveEventsRequest
import com.tsbonev.cqrs.core.eventstore.SaveEventsResponse
import com.tsbonev.cqrs.core.eventstore.SaveOptions
import com.tsbonev.cqrs.core.snapshot.Snapshot
import java.util.LinkedList

class InMemoryEventStore(private val eventsLimit: Int) : EventStore {
	private val idToAggregate = mutableMapOf<String, MutableList<StoredAggregate>>()
	private val stubbedResponses = LinkedList<SaveEventsResponse>()
	public val saveEventOptions = LinkedList<SaveOptions>()


	override fun saveEvents(request: SaveEventsRequest, saveOptions: SaveOptions): SaveEventsResponse {
		saveEventOptions.add(saveOptions)

		if (stubbedResponses.size > 0) {
			return stubbedResponses.pop()
		}

		val streamKey = streamKey(request.tenant, request.stream)

		val aggregate = if (!idToAggregate.contains(streamKey)) {
			val newAggr = StoredAggregate(request.tenant, request.aggregateType, mutableListOf(), null)
			idToAggregate[streamKey] = mutableListOf(newAggr)
			newAggr
		} else {
			idToAggregate[streamKey]!!.find { it.events.find { event -> event.aggregateId == request.events[0].aggregateId} != null }!!
		}

		val aggregates = idToAggregate[streamKey]!!

		if (saveOptions.createSnapshotRequest.required) {
			val snapshot = saveOptions.createSnapshotRequest.snapshot
			aggregates.add(StoredAggregate(request.tenant, request.aggregateType, mutableListOf(), snapshot))
		} else if (aggregate.events.size + request.events.size > eventsLimit) {
			return SaveEventsResponse.SnapshotRequired(aggregate.events, aggregate.snapshot, aggregate.events.size.toLong())
		}

		aggregate.events.addAll(request.events)

		val version = aggregate.events.size.toLong() + (aggregate.snapshot?.version ?: 0L)
		return SaveEventsResponse.Success(
			version,
			(0..request.events.size).map { it.toLong() },
			ConcreteAggregate(aggregate.aggregateType, aggregate.snapshot, version, aggregate.events)
		)
	}

	override fun getEventsFromStreams(request: GetEventsFromStreamsRequest): GetEventsResponse {
		val aggregates = mutableListOf<ConcreteAggregate>()
		request.streams.forEach { stream ->
			val streamKey = streamKey(request.tenant, stream)

			idToAggregate[streamKey]?.forEach {

				val aggregateIdToEvents = it.events.groupBy { it.aggregateId }
				aggregateIdToEvents.forEach { aggregateId, events ->

					aggregates.add(
						ConcreteAggregate(
							it.aggregateType,
							it.snapshot,
							it.events.size.toLong() + (it.snapshot?.version ?: 0L),
							it.events
						)
					)
				}
			}
		}

		return GetEventsResponse.Success(aggregates)
	}

	override fun getAllEvents(request: GetAllEventsRequest): GetAllEventsResponse {
		var positionId = 1L
		val result = mutableListOf<IndexedEvent>()

		idToAggregate.values.forEach { aggregates ->
			aggregates.forEach { aggregate ->
				aggregate.events.forEach { event ->
					result.add(IndexedEvent(Position(positionId), aggregate.tenant, aggregate.aggregateType, positionId, event))
					positionId++
				}
			}
		}
		return GetAllEventsResponse.Success(result, request.readDirection, Position(positionId))
	}

	override fun revertEvents(tenant: String, stream: String, count: Int): RevertEventsResponse {
		val streamKey = streamKey(tenant, stream)

		if (!idToAggregate.containsKey(streamKey)) {
			return RevertEventsResponse.AggregateNotFound(tenant, stream)
		}

		val aggregates = idToAggregate[streamKey]!!
		val aggregate = aggregates[0]

		val lastEventIndex = aggregate.events.size - count

		val updatedEvents = aggregate.events.filterIndexed { index, _ -> index < lastEventIndex }.toMutableList()

		idToAggregate[streamKey] = mutableListOf(StoredAggregate(aggregate.tenant, aggregate.aggregateType, updatedEvents, aggregate.snapshot))

		return RevertEventsResponse.Success(listOf())
	}

	fun pretendThatNextSaveWillReturn(response: SaveEventsResponse) {
		stubbedResponses.add(response)
	}

	private fun streamKey(tenant: String, stream: String) = "$tenant:$stream"

}

private data class StoredAggregate(val tenant: String, val aggregateType: String, val events: MutableList<EventPayload>, val snapshot: Snapshot?)