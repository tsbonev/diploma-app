package com.tsbonev.cqrs.core.eventstore

interface EventStore {
	fun saveEvents(
		aggregateIdentity: AggregateIdentity,
		events: Events,
		saveOptions: SaveOptions
	): SaveEventsResponse

	fun getEvents(aggregateId: String): EventSourcedAggregate

	fun getEvents(aggregateIds: List<String>): List<EventSourcedAggregate>

	fun revertToVersion(aggregateIdentity: AggregateIdentity): RevertEventsResponse
}