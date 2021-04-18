package com.tsbonev.cqrs.core.helpers

import com.tsbonev.cqrs.core.AggregateNotFoundException
import com.tsbonev.cqrs.core.AggregateRoot
import com.tsbonev.cqrs.core.Aggregates
import com.tsbonev.cqrs.core.messagebus.Event

class InMemoryAggregateRepository : Aggregates {
	private val aggregateIdToEvents = mutableMapOf<String, MutableList<AggregatedEvent>>()

	override fun <T : AggregateRoot> save(aggregate: T) {
		val changes = aggregate.getEvents().toMutableList()
		val events = changes.map { AggregatedEvent(aggregate.getId(), it) }.toMutableList()
		if (aggregateIdToEvents.containsKey(aggregate.getId())) {
			aggregateIdToEvents[aggregate.getId()]!!.addAll(events)
		} else {
			aggregateIdToEvents[aggregate.getId()] = events
		}

		aggregate.commitEvents()
	}

	override fun <T : AggregateRoot> getById(aggregateId: String, type: Class<T>): T {
		if (!aggregateIdToEvents.containsKey(aggregateId)) {
			throw AggregateNotFoundException("Cannot find aggregate with ID '$aggregateId'")
		}
		val events = aggregateIdToEvents[aggregateId]!!.map { it.event }

		val instance = type.newInstance() as T
		instance.buildFromHistory(events, events.size.toLong())
		return instance
	}

	override fun <T : AggregateRoot> getByIds(ids: List<String>, type: Class<T>): Map<String, T> {
		return ids.filter { aggregateIdToEvents.containsKey(it) }.map { aggregateEvents ->
			val instance = type.newInstance() as T
			val events = aggregateIdToEvents[aggregateEvents]!!.map { it.event }
			instance.buildFromHistory(events, events.size.toLong())
			Pair(aggregateEvents, instance)
		}.toMap()
	}
}

data class AggregatedEvent(val aggregateId: String, val event: Event)