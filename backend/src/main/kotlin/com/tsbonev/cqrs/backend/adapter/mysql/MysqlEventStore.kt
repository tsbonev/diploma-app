package com.tsbonev.cqrs.backend.adapter.mysql

import com.tsbonev.cqrs.core.AggregateNotFoundException
import com.tsbonev.cqrs.core.eventstore.AggregateIdentity
import com.tsbonev.cqrs.core.eventstore.EventSourcedAggregate
import com.tsbonev.cqrs.core.eventstore.EventStore
import com.tsbonev.cqrs.core.eventstore.Events
import com.tsbonev.cqrs.core.eventstore.RevertEventsResponse
import com.tsbonev.cqrs.core.eventstore.SaveEventsResponse
import com.tsbonev.cqrs.core.eventstore.SaveOptions
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
class MysqlEventStore(
	@Autowired private val aggregateRepository: MysqlAggregateRepository,
	@Autowired private val eventsRepository: MysqlEventRepository,
	@Autowired private val snapshotsRepository: MysqlSnapshotRepository,
	private val eventsLimit: Int = 100
) : EventStore {
	override fun saveEvents(aggregateIdentity: AggregateIdentity, events: Events, saveOptions: SaveOptions): SaveEventsResponse {
		val aggregateEntity = aggregateRepository.findById(aggregateIdentity.aggregateId)

		val currentAggregate = if (aggregateEntity.isPresent) aggregateEntity.get().toEventSourcedAggregate() else
			EventSourcedAggregate(
				AggregateIdentity(aggregateIdentity.aggregateId, aggregateIdentity.aggregateType, 0),
				Events(aggregateIdentity.aggregateId), null
			)


		val newEvents = currentAggregate.events.events.plus(events.events).mapIndexed { index, eventWithContext ->
			eventWithContext.copy(version = index.toLong())
		}


		if (currentAggregate.snapshot == null && newEvents.size - (currentAggregate.snapshot?.version
				?: 0L) >= eventsLimit) {
			return SaveEventsResponse.SnapshotRequired(events, currentAggregate.snapshot)
		}

		val eventsFinalVersion = newEvents.size.toLong() - 1

		val updatedVersion = currentAggregate.copy(
			aggregateIdentity = currentAggregate.aggregateIdentity.copy(
				aggregateVersion = eventsFinalVersion
			)
		)

		val updatedEvents = updatedVersion.copy(
			events = updatedVersion.events.copy(
				finalVersion = eventsFinalVersion,
				events = newEvents
			)
		)

		val updatedSnapshot = if (saveOptions.snapshot != null) {
			updatedEvents.copy(snapshot = saveOptions.snapshot)
		} else updatedEvents

		val updatedEntity = AggregateEntity.fromEventSourcedAggregate(updatedSnapshot)

		eventsRepository.saveAll(updatedEntity.events)
		if (updatedEntity.snapshot != null) snapshotsRepository.save(updatedEntity.snapshot)
		aggregateRepository.save(updatedEntity)

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
		val aggregateEntity = aggregateRepository.findById(aggregateId)

		return if (aggregateEntity.isPresent) aggregateEntity.get().toEventSourcedAggregate()
		else throw AggregateNotFoundException(aggregateId)
	}

	override fun getEvents(aggregateIds: List<String>): List<EventSourcedAggregate> {
		val aggregateEntities = aggregateRepository.findAllById(aggregateIds)

		return aggregateEntities.map { it.toEventSourcedAggregate() }
	}

	override fun revertToVersion(aggregateIdentity: AggregateIdentity): RevertEventsResponse {
		TODO("Not yet implemented")
	}
}