package com.tsbonev.cqrs.core.eventstore

import com.tsbonev.cqrs.core.snapshot.Snapshot
import java.time.Instant

data class AggregateIdentity(val aggregateId: String, val aggregateType: String = "", val aggregateVersion: Long = 0L)

data class Events(val aggregateId: String, val finalVersion: Long = 0L, val events: List<EventWithContext> = listOf())

data class EventWithContext(val eventData: ByteArray, val kind: String, val version: Long, val creationContext: CreationContext) {
	override fun equals(other: Any?): Boolean {
		if (this === other) return true
		if (javaClass != other?.javaClass) return false

		other as EventWithContext

		if (!eventData.contentEquals(other.eventData)) return false
		if (version != other.version) return false
		if (creationContext != other.creationContext) return false

		return true
	}

	override fun hashCode(): Int {
		var result = eventData.contentHashCode()
		result = 31 * result + version.hashCode()
		result = 31 * result + creationContext.hashCode()
		return result
	}
}

data class CreationContext(val user: User = User("::stub::"), val instant: Instant = Instant.MIN)

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
