package com.tsbonev.cqrs.core.eventstore

import com.tsbonev.cqrs.core.snapshot.Snapshot


interface EventStore {
	fun saveEvents(request: SaveEventsRequest, saveOptions: SaveOptions = SaveOptions(version = 0L)): SaveEventsResponse

	fun getEventsFromStreams(request: GetEventsFromStreamsRequest): GetEventsResponse

	fun getAllEvents(request: GetAllEventsRequest): GetAllEventsResponse

	fun revertEvents(tenant: String, stream: String, count: Int): RevertEventsResponse
}

/**
 * Persist events as is.
 */
data class SaveEventsRequest(val tenant: String,
                             val stream: String,
                             val aggregateType: String,
                             val events: List<EventPayload>
)

sealed class SaveEventsResponse {
	data class Success(val version: Long,
	                   val sequenceIds: List<Long>,
	                   val aggregate: ConcreteAggregate) : SaveEventsResponse()

	data class EventCollision(val expectedVersion: Long) : SaveEventsResponse()

	data class CommunicationError(val message: String) : SaveEventsResponse()

	data class SnapshotRequired(val currentEvents: List<EventPayload>,
	                            val currentSnapshot: Snapshot? = null,
	                            val version: Long) : SaveEventsResponse()

	data class Error(val message: String) : SaveEventsResponse()
}


data class GetEventsFromStreamsRequest(val tenant: String, val streams: List<String>) {
	constructor(tenant: String, stream: String) : this(tenant, listOf(stream))
}

data class GetAllEventsRequest(
	val position: Position? = Position(0),
	val maxCount: Int = 100,
	val readDirection: ReadDirection = ReadDirection.FORWARD,
	val streams: List<String> = listOf()
)

sealed class GetAllEventsResponse {
	data class Success(val events:List<IndexedEvent>,
	                   val readDirection: ReadDirection,
	                   val nextPosition: Position?) : GetAllEventsResponse()

	data class ErrorInCommunication(val message: String) : GetAllEventsResponse()

	data class Error(val message: String) : GetAllEventsResponse()
}

sealed class GetEventsResponse {

	data class Success(val aggregates: List<ConcreteAggregate>) : GetEventsResponse()

	data class SnapshotNotFound(val aggregateId: String, val aggregateType: String) : GetEventsResponse()

	data class AggregateNotFound(val aggregateIds: List<String>, val aggregateType: String) : GetEventsResponse()

	data class CommunicationError(val message: String) : GetEventsResponse()

	data class Error(val message: String) : GetEventsResponse()
}

sealed class RevertEventsResponse {

	data class Success(val eventIds: List<String>) : RevertEventsResponse()

	data class AggregateNotFound(val aggregateId: String, val aggregateType: String) : RevertEventsResponse()

	data class InsufficientEventsError(val available: Int, val requested: Int) : RevertEventsResponse()

	data class Error(val message: String) : RevertEventsResponse()
}
