package com.tsbonev.cqrs.core.eventstore

import com.tsbonev.cqrs.core.snapshot.Snapshot

data class AggregateIdentity(val aggregateId: String, val aggregateType: String = "", val aggregateVersion: Long = 0L)

data class Events(val aggregateId: String, val finalVersion: Long = 0L, val events: List<EventWithContext> = listOf())

data class EventWithContext(val eventData: Any, val kind: String, val version: Long, val creationContext: CreationContext)

data class CreationContext(val user: User = User("::stub::"), val instant: Long = 0L)

data class User(val id: String = "")

/**
 * If a snapshot is given, the EventStore should persist it for the given aggregate.
 */
data class SaveOptions(val snapshot: Snapshot? = null)

sealed class SaveEventsResponse {
	data class Success(val aggregate: EventSourcedAggregate) : SaveEventsResponse()

	data class EventCollision(val expectedVersion: Long, val actualVersion: Long) : SaveEventsResponse()

	data class CommunicationError(val message: String) : SaveEventsResponse()

	data class SnapshotRequired(val currentEvents: Events,
	                            val currentSnapshot: Snapshot? = null) : SaveEventsResponse()

	data class Error(val message: String) : SaveEventsResponse()
}


data class EventSourcedAggregate(val aggregateIdentity: AggregateIdentity, val events: Events, val snapshot: Snapshot? = null)

sealed class RevertEventsResponse {

	data class Success(val eventIds: List<EventWithContext>) : RevertEventsResponse()

	data class AggregateNotFound(val aggregateId: AggregateIdentity) : RevertEventsResponse()

	data class CannotRevertEventForward(val availableVersion: Long, val requestedVersion: Long) : RevertEventsResponse()

	data class Error(val message: String) : RevertEventsResponse()
}
