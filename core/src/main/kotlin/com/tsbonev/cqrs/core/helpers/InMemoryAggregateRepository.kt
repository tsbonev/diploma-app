package com.tsbonev.cqrs.core.helpers

import com.tsbonev.cqrs.core.AggregateNotFoundException
import com.tsbonev.cqrs.core.AggregateRepository
import com.tsbonev.cqrs.core.AggregateRoot

class InMemoryAggregateRepository : AggregateRepository {
	private val aggregateIdToEvents = mutableMapOf<String, MutableList<AggregatedEvent>>()

	override fun <T : AggregateRoot> save(aggregate: T) {
		val changes = aggregate.getEvents().toMutableList()
		val events = changes.map { AggregatedEvent(aggregate.getId()!!, it) }.toMutableList()
		if (aggregateIdToEvents.containsKey(aggregate.getId())) {
			aggregateIdToEvents[aggregate.getId()]!!.addAll(events)
		} else {
			aggregateIdToEvents[aggregate.getId()!!] = events
		}

		aggregate.commitEvents()
	}

	override fun <T : AggregateRoot> save(stream: String, aggregate: T) {
		val changes = aggregate.getEvents().toMutableList()
		val events = changes.map { AggregatedEvent(aggregate.getId()!!, it) }.toMutableList()
		if (aggregateIdToEvents.containsKey(stream)) {
			aggregateIdToEvents[stream]!!.addAll(events)
		} else {
			aggregateIdToEvents[stream] = events
		}

		aggregate.commitEvents()

	}

	override fun <T : AggregateRoot> getById(id: String, type: Class<T>): T {
		if (!aggregateIdToEvents.containsKey(id)) {
			throw AggregateNotFoundException("Cannot find aggregate with ID '$id'")
		}
		val events = aggregateIdToEvents[id]!!.map { it.event }

		val instance = type.newInstance() as T
		instance.buildFromHistory(events, events.size.toLong())
		return instance
	}

	override fun <T : AggregateRoot> getById(stream: String, aggregateId: String, type: Class<T>): T {
		if (!aggregateIdToEvents.containsKey(stream)) {
			throw AggregateNotFoundException("Cannot find aggregate with ID '$stream'")
		}
		val events = aggregateIdToEvents[stream]!!.filter { it.aggregateId == aggregateId }.map { it.event }

		val instance = type.newInstance() as T
		instance.buildFromHistory(events, events.size.toLong())
		return instance
	}

	override fun <T : AggregateRoot> getByIds(ids: List<String>, type: Class<T>): Map<String, T> {
		return ids.filter { aggregateIdToEvents.containsKey(it) }.map {
			val instance = type.newInstance() as T
			val events = aggregateIdToEvents[it]!!.map { it.event }
			instance.buildFromHistory(events, events.size.toLong())
			Pair(it, instance)
		}.toMap()
	}
}

data class AggregatedEvent(val aggregateId: String, val event: Any)