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
	private var eventsLimit: Int = 5
) : EventStore {

	fun setEventLimit(limit: Int) {
		eventsLimit = limit
	}

	override fun saveEvents(aggregateIdentity: AggregateIdentity, events: Events, saveOptions: SaveOptions): SaveEventsResponse {
		val aggregateEntity = aggregateRepository.findById(aggregateIdentity.aggregateId)

		val currentIdentity = if (aggregateEntity.isPresent) aggregateEntity.get().toAggregateIdentity()
		else AggregateIdentity(aggregateIdentity.aggregateId, aggregateIdentity.aggregateType, 0)

		val currentSnapshot = if(saveOptions.snapshot != null) saveOptions.snapshot
		else snapshotsRepository.findById(aggregateIdentity.aggregateId).orElseGet { null }?.toSnapshot()

		val queriedEvents = if(currentSnapshot != null) {
			eventsRepository.findByAggregateIdAndVersionGreaterThan(aggregateIdentity.aggregateId, currentSnapshot.version)
		} else {
			eventsRepository.findByAggregateId(aggregateIdentity.aggregateId)
		}

		val currentEvents = if(queriedEvents.isEmpty()) {
			Events(aggregateIdentity.aggregateId, 0L, listOf())
		} else {
			queriedEvents.toEvents()
		}

		// Overlapping versions
		if (currentIdentity.aggregateVersion != 0L
			&& currentEvents.finalVersion >= events.finalVersion) {
			return SaveEventsResponse.EventCollision(currentEvents.finalVersion + 1, events.finalVersion)
		}

		val offset = (currentSnapshot?.version ?: -1L) + 1L

		val newEvents = currentEvents.events.plus(events.events).mapIndexed { index, eventWithContext ->
			eventWithContext.copy(version = index.toLong() + offset)
		}

		// Snapshot check
		if (saveOptions.snapshot == null && newEvents.size - (currentSnapshot?.version
				?: 0L) >= eventsLimit) {
			return SaveEventsResponse.SnapshotRequired(currentEvents, currentSnapshot)
		}

		val updatedIdentity = currentIdentity.copy(
			aggregateVersion = events.finalVersion
		)

		val updatedEvents = currentEvents.copy(
			finalVersion = events.finalVersion,
			events = newEvents
		)

		val updatedSnapshot = if (saveOptions.snapshot != null) {
			saveOptions.snapshot
		} else currentSnapshot

		val snapshotEntity = SnapshotEntity.fromSnapshot(aggregateIdentity.aggregateId, updatedSnapshot)
		if(snapshotEntity != null) snapshotsRepository.save(snapshotEntity)

		aggregateRepository.save(AggregateEntity.fromAggregateIdentity(updatedIdentity))
		eventsRepository.saveAll(EventEntity.fromEvents(updatedEvents))

		return SaveEventsResponse.Success(
			EventSourcedAggregate(
				updatedIdentity,
				updatedEvents,
				updatedSnapshot
			)
		)
	}

	override fun getEvents(aggregateId: String): EventSourcedAggregate {
		val aggregateEntity = aggregateRepository.findById(aggregateId)

		val currentIdentity = if (aggregateEntity.isPresent) aggregateEntity.get().toAggregateIdentity()
		else throw AggregateNotFoundException(aggregateId)

		val currentSnapshot = snapshotsRepository.findById(aggregateId).orElseGet { null }?.toSnapshot()

		val queriedEvents = if(currentSnapshot != null) {
			eventsRepository.findByAggregateIdAndVersionGreaterThan(aggregateId, currentSnapshot.version)
		} else {
			eventsRepository.findByAggregateId(aggregateId)
		}

		val currentEvents = if(queriedEvents.isEmpty()) {
			Events(aggregateId, 0L, listOf())
		} else {
			queriedEvents.toEvents()
		}

		return EventSourcedAggregate(
			currentIdentity,
			currentEvents,
			currentSnapshot
		)
	}

	override fun getEvents(aggregateIds: List<String>): List<EventSourcedAggregate> {
		val aggregateEntities = aggregateRepository.findAllById(aggregateIds)

		return aggregateEntities.map { aggregateEntity ->
			val currentIdentity = aggregateEntity.toAggregateIdentity()

			val currentSnapshot = snapshotsRepository.findById(aggregateEntity.aggregateId).orElseGet { null }?.toSnapshot()

			val queriedEvents = if(currentSnapshot != null) {
				eventsRepository.findByAggregateIdAndVersionGreaterThan(aggregateEntity.aggregateId, currentSnapshot.version)
			} else {
				eventsRepository.findByAggregateId(aggregateEntity.aggregateId)
			}

			val currentEvents = if(queriedEvents.isEmpty()) {
				Events(aggregateEntity.aggregateId, 0L, listOf())
			} else {
				queriedEvents.toEvents()
			}

			EventSourcedAggregate(
				currentIdentity,
				currentEvents,
				currentSnapshot
			)
		}
	}

	override fun revertToVersion(aggregateIdentity: AggregateIdentity): RevertEventsResponse {
		val aggregateEntity = aggregateRepository.findById(aggregateIdentity.aggregateId)

		val currentIdentity = if (aggregateEntity.isPresent) aggregateEntity.get().toAggregateIdentity()
		else return RevertEventsResponse.AggregateNotFound(aggregateIdentity)

		val currentSnapshot = snapshotsRepository.findById(aggregateIdentity.aggregateId).orElseGet { null }?.toSnapshot()

		val currentEvents = if(currentSnapshot != null) {
			eventsRepository.findByAggregateIdAndVersionGreaterThan(aggregateIdentity.aggregateId, currentSnapshot.version)
		} else {
			eventsRepository.findByAggregateId(aggregateIdentity.aggregateId)
		}.toEvents()


		if (currentSnapshot != null && currentSnapshot.version > aggregateIdentity.aggregateVersion) {
			snapshotsRepository.deleteById(aggregateIdentity.aggregateId)
		}

		return if (currentIdentity.aggregateVersion < aggregateIdentity.aggregateVersion) {
			RevertEventsResponse.CannotRevertEventForward(
				currentIdentity.aggregateVersion,
				aggregateIdentity.aggregateVersion
			)
		} else {
			if (currentIdentity.aggregateVersion < aggregateIdentity.aggregateVersion) {
				return RevertEventsResponse.CannotRevertEventForward(
					currentEvents.finalVersion,
					aggregateIdentity.aggregateVersion
				)
			}

			val eventsToBeReverted = currentEvents.events.filter { it.version > aggregateIdentity.aggregateVersion }

			val updatedIdentity = currentIdentity.copy(
				aggregateVersion = aggregateIdentity.aggregateVersion
			)

			aggregateRepository.save(AggregateEntity.fromAggregateIdentity(updatedIdentity))

			eventsRepository.deleteAll(
				EventEntity.fromEvents(
					Events(
						aggregateIdentity.aggregateId,
						aggregateIdentity.aggregateVersion, eventsToBeReverted
					)
				)
			)

			RevertEventsResponse.Success(eventsToBeReverted)
		}
	}
}