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
	private var eventsLimit: Int = 500
) : EventStore {

	fun setEventLimit(limit: Int) {
		eventsLimit = limit
	}

	override fun saveEvents(aggregateIdentity: AggregateIdentity, events: Events, saveOptions: SaveOptions): SaveEventsResponse {
		val aggregateEntity = aggregateRepository.findById(aggregateIdentity.aggregateId)

		val currentAggregate = if (aggregateEntity.isPresent) aggregateEntity.get().toEventSourcedAggregate() else
			EventSourcedAggregate(
				AggregateIdentity(aggregateIdentity.aggregateId, aggregateIdentity.aggregateType, 0),
				Events(aggregateIdentity.aggregateId), null
			)

		// Overlapping versions
		if (currentAggregate.aggregateIdentity.aggregateVersion != 0L
			&& currentAggregate.events.finalVersion >= events.finalVersion) {
			return SaveEventsResponse.EventCollision(currentAggregate.events.finalVersion + 1, events.finalVersion)
		}

		val expectedFirstEventVersion = currentAggregate.aggregateIdentity.aggregateVersion + 1
		val firstEventVersion = events.events.minByOrNull { it.version }?.version ?: -1L

		// Missing versions
		if (firstEventVersion != 0L && expectedFirstEventVersion != firstEventVersion) {
			println("Collision for $expectedFirstEventVersion expected but is $firstEventVersion")
			return SaveEventsResponse.EventCollision(expectedFirstEventVersion, firstEventVersion)
		}


		val newEvents = currentAggregate.events.events.plus(events.events).mapIndexed { index, eventWithContext ->
			eventWithContext.copy(version = index.toLong())
		}

		// Snapshot check
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

		if (updatedEntity.snapshot != null) snapshotsRepository.save(updatedEntity.snapshot)
		aggregateRepository.save(updatedEntity)
		eventsRepository.saveAll(updatedEntity.events)

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

		return if (aggregateEntity.isPresent) aggregateEntity.get().toEventSourcedAggregate(saving = false)
		else throw AggregateNotFoundException(aggregateId)
	}

	override fun getEvents(aggregateIds: List<String>): List<EventSourcedAggregate> {
		val aggregateEntities = aggregateRepository.findAllById(aggregateIds)

		return aggregateEntities.map { it.toEventSourcedAggregate(saving = false) }
	}

	override fun revertToVersion(aggregateIdentity: AggregateIdentity): RevertEventsResponse {
		val aggregateEntity = aggregateRepository.findById(aggregateIdentity.aggregateId)

		val aggregate = if (aggregateEntity.isPresent) aggregateEntity.get().toEventSourcedAggregate()
		else return RevertEventsResponse.AggregateNotFound(aggregateIdentity)

		val snapshot = aggregate.snapshot

		if (snapshot != null && snapshot.version > aggregateIdentity.aggregateVersion) {
			snapshotsRepository.deleteById(aggregateIdentity.aggregateId)
		}

		return if (aggregate.aggregateIdentity.aggregateVersion < aggregateIdentity.aggregateVersion) {
			RevertEventsResponse.CannotRevertEventForward(
				aggregate.aggregateIdentity.aggregateVersion,
				aggregateIdentity.aggregateVersion
			)
		} else {
			if (aggregate.aggregateIdentity.aggregateVersion < aggregateIdentity.aggregateVersion) {
				return RevertEventsResponse.CannotRevertEventForward(
					aggregate.events.finalVersion,
					aggregateIdentity.aggregateVersion
				)
			}

			val eventsToBeReverted = aggregate.events.events.filter { it.version > aggregateIdentity.aggregateVersion }

			val revertedEvents = aggregate.events.copy(finalVersion = aggregateIdentity.aggregateVersion,
			                                           events = aggregate.events.events.filter {
				                                           it.version <= aggregateIdentity.aggregateVersion
			                                           })

			val updatedEvents = aggregate.copy(
				events = revertedEvents,
				aggregateIdentity = aggregate.aggregateIdentity.copy(aggregateVersion = aggregateIdentity.aggregateVersion),
				snapshot = if(snapshot != null && snapshot.version > aggregateIdentity.aggregateVersion) null else aggregate.snapshot
			)

			aggregateRepository.save(AggregateEntity.fromEventSourcedAggregate(updatedEvents))

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