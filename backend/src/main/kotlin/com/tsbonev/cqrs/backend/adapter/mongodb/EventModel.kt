package com.tsbonev.cqrs.backend.adapter.mongodb

data class EventModel(
	@JvmField val aggregateId: String,
	@JvmField val kind: String,
	@JvmField val version: Long,
	@JvmField val identityId: String,
	@JvmField val timestamp: Long,
	@JvmField val payload: String
)

data class EventsModel(@JvmField val events: List<EventModel>) {
	/**
	 * Removes last N events from list.
	 */
	fun removeLastN(count: Int): EventsModel {
		val lastEventIndex = events.size - count
		val updatedEvents = events.filterIndexed { index, _ -> index < lastEventIndex }
		return EventsModel(updatedEvents)
	}
}