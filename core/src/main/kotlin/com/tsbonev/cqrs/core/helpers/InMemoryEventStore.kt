package com.tsbonev.cqrs.core.helpers

import com.tsbonev.cqrs.core.AggregateNotFoundException
import com.tsbonev.cqrs.core.eventstore.AggregateIdentity
import com.tsbonev.cqrs.core.eventstore.EventSourcedAggregate
import com.tsbonev.cqrs.core.eventstore.EventStore
import com.tsbonev.cqrs.core.eventstore.Events
import com.tsbonev.cqrs.core.eventstore.RevertEventsResponse
import com.tsbonev.cqrs.core.eventstore.SaveEventsResponse
import com.tsbonev.cqrs.core.eventstore.SaveOptions
import com.tsbonev.cqrs.core.snapshot.Snapshot
import java.util.LinkedList

class InMemoryEventStore(private val eventsLimit: Int) : EventStore {
	private val aggregatesMap = mutableMapOf<String, AggregateIdentity>()
	private val eventsMap = mutableMapOf<String, Events>()
	private val snapshotsMap = mutableMapOf<String, Snapshot>()
	private val stubbedResponses = mutableListOf<SaveEventsResponse>()

	val saveEventOptions = LinkedList<SaveOptions>()

	fun pretendThatNextSaveWillReturn(response: SaveEventsResponse) {
		stubbedResponses.add(response)
	}

	override fun saveEvents(aggregateIdentity: AggregateIdentity, events: Events, saveOptions: SaveOptions): SaveEventsResponse {
		if(stubbedResponses.isNotEmpty()) {
			val response = stubbedResponses.first()
			stubbedResponses.clear()

			return response
		}

		val currentSnapshot = saveOptions.snapshot ?: snapshotsMap[aggregateIdentity.aggregateId]

		val currentAggregate = aggregatesMap[aggregateIdentity.aggregateId]
			?: AggregateIdentity(aggregateIdentity.aggregateId, aggregateIdentity.aggregateType, 0)
		val currentEvents = eventsMap[aggregateIdentity.aggregateId] ?: Events(
			aggregateIdentity.aggregateId,
			0,
			listOf()
		)

		val newEvents = currentEvents.events.plus(events.events).mapIndexed { index, eventWithContext ->
			eventWithContext.copy(version = index.toLong())
		}


		if (currentSnapshot == null && newEvents.size - (currentSnapshot?.version ?: 0L) >= eventsLimit) {
			return SaveEventsResponse.SnapshotRequired(events, currentSnapshot)
		}

		val eventsFinalVersion = newEvents.size.toLong()

		aggregatesMap[aggregateIdentity.aggregateId] = currentAggregate.copy(aggregateVersion = eventsFinalVersion)

		eventsMap[aggregateIdentity.aggregateId] = currentEvents.copy(
			finalVersion = eventsFinalVersion,
			events = newEvents
		)

		if (saveOptions.snapshot != null) {
			snapshotsMap[aggregateIdentity.aggregateId] = saveOptions.snapshot
		}

		return SaveEventsResponse.Success(
			EventSourcedAggregate(
				aggregateIdentity.copy(),
				Events(
					events.aggregateId,
					eventsFinalVersion,
					events.events
				),
				saveOptions.snapshot
			)
		)
	}

	override fun getEvents(aggregateId: String): EventSourcedAggregate {
		val aggregateIdentity = aggregatesMap[aggregateId]

		val snapshot = snapshotsMap[aggregateId]

		val events = eventsMap[aggregateId]

		if(events == null || aggregateIdentity == null) {
			throw AggregateNotFoundException(aggregateId)
		}

		return EventSourcedAggregate(aggregateIdentity, events, snapshot)
	}

	override fun getEvents(aggregateIds: List<String>): List<EventSourcedAggregate> {
		val aggregates = mutableListOf<EventSourcedAggregate>()

		aggregateIds.forEach { aggregateId ->
			val aggregateIdentity = aggregatesMap[aggregateId]

			val snapshot = snapshotsMap[aggregateId]

			val events = eventsMap[aggregateId]

			if(aggregateIdentity != null && events != null) {
				aggregates.add(EventSourcedAggregate(aggregateIdentity, events, snapshot))
			}
		}

		return aggregates
	}

	override fun revertToVersion(aggregateIdentity: AggregateIdentity): RevertEventsResponse {
		val currentSnapshot = snapshotsMap[aggregateIdentity.aggregateId]

		if (currentSnapshot != null && currentSnapshot.version > aggregateIdentity.aggregateVersion) {
			snapshotsMap.remove(aggregateIdentity.aggregateId)
		}

		/**
		 * Should never throw, if it does, then something very bad has happened.
		 */
		val currentAggregate = aggregatesMap[aggregateIdentity.aggregateId]
			?: return RevertEventsResponse.AggregateNotFound(aggregateIdentity)

		return if (currentAggregate.aggregateVersion < aggregateIdentity.aggregateVersion) {
			RevertEventsResponse.CannotRevertEventForward(
				currentAggregate.aggregateVersion,
				aggregateIdentity.aggregateVersion
			)
		} else {
			val events = eventsMap[aggregateIdentity.aggregateId]
				?: return RevertEventsResponse.Error("No events found.")

			if (events.finalVersion < aggregateIdentity.aggregateVersion) return RevertEventsResponse.CannotRevertEventForward(
				events.finalVersion,
				aggregateIdentity.aggregateVersion
			)

			val eventsToBeReverted = events.events.filter { it.version > aggregateIdentity.aggregateVersion }

			val revertedEvents = events.copy(finalVersion = aggregateIdentity.aggregateVersion,
			                                 events = events.events.filter {
				                                 it.version <= aggregateIdentity.aggregateVersion
			                                 })

			eventsMap[aggregateIdentity.aggregateId] = revertedEvents
			aggregatesMap[aggregateIdentity.aggregateId] = currentAggregate.copy(aggregateVersion = aggregateIdentity.aggregateVersion)

			RevertEventsResponse.Success(eventsToBeReverted)
		}
	}
}