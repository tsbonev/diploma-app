package com.tsbonev.cqrs.core

import com.tsbonev.cqrs.core.eventstore.AggregateIdentity
import com.tsbonev.cqrs.core.eventstore.CreationContext
import com.tsbonev.cqrs.core.eventstore.EventPublisher
import com.tsbonev.cqrs.core.eventstore.EventStore
import com.tsbonev.cqrs.core.eventstore.EventWithContext
import com.tsbonev.cqrs.core.eventstore.Events
import com.tsbonev.cqrs.core.eventstore.SaveEventsResponse
import com.tsbonev.cqrs.core.eventstore.SaveOptions
import com.tsbonev.cqrs.core.eventstore.User
import com.tsbonev.cqrs.core.messagebus.Event
import com.tsbonev.cqrs.core.snapshot.MessageFormat
import com.tsbonev.cqrs.core.snapshot.Snapshot
import java.util.UUID


class SimpleIdentityAggregateRepository(
	private val eventStore: EventStore,
	private val messageFormat: MessageFormat<Any>,
	private val eventPublisher: EventPublisher
) : IdentityAggregateRepository {

	@Throws(EventCollisionException::class, PublishErrorException::class)
	override fun <T : AggregateRoot> save(aggregate: T, identity: Identity) {
		var aggregateId = aggregate.getId()
		if (aggregateId == "") {
			aggregateId = UUID.randomUUID().toString()
		}
		return this.save(aggregateId, aggregate, identity)
	}

	override fun <T : AggregateRoot> getById(aggregateId: String, type: Class<T>, identity: Identity): T {
		val response = eventStore.getEvents(aggregateId)

		return buildAggregateFromHistory(
			aggregateId,
			type,
			response.aggregateIdentity.aggregateVersion,
			response.events,
			response.snapshot
		)
	}

	override fun <T : AggregateRoot> getByIds(ids: List<String>, type: Class<T>, identity: Identity): Map<String, T> {
		if (ids.isEmpty()) {
			return mapOf()
		}

		val aggregates = eventStore.getEvents(ids)

		val result = mutableMapOf<String, T>()
		aggregates.forEach { response ->
			val aggregateId = response.aggregateIdentity.aggregateId
			result[aggregateId] = buildAggregateFromHistory(
				aggregateId,
				type,
				response.aggregateIdentity.aggregateVersion,
				response.events,
				response.snapshot
			)
		}
		return result
	}

	private fun <T : AggregateRoot> save(aggregateId: String, aggregate: T, identity: Identity) {
		val uncommittedEvents = aggregate.getEvents()

		val initialVersion = aggregate.getExpectedVersion()

		val events = uncommittedEvents.map {
			EventWithContext(
				messageFormat.format(it),
				it::class.java.simpleName,
				0L,
				CreationContext(User(identity.id), identity.time)
			)
		}

		/**
		 * If there are no news changes, the aggregate doesn't need updating.
		 */
		if (events.isEmpty()) return

		val aggregateClass = aggregate::class.java

		val finalVersion = (if(initialVersion == 0L) -1L else initialVersion) + if(initialVersion != 0L) events.size -1 else events.size

		val response = eventStore.saveEvents(
			AggregateIdentity(aggregateId, aggregateClass.simpleName, initialVersion),
			Events(aggregateId, finalVersion, events),
			SaveOptions(null)
		)

		when (response) {
			is SaveEventsResponse.Success -> {
				try {
					eventPublisher.publish(events)
					aggregate.commitEvents()
				} catch (ex: PublishErrorException) {
					eventStore.revertToVersion(AggregateIdentity(aggregateId, aggregateClass.simpleName, initialVersion - events.size))
					throw ex
				}
			}

			is SaveEventsResponse.EventCollision -> {
				throw EventCollisionException(aggregateId, response.expectedVersion)
			}

			is SaveEventsResponse.SnapshotRequired -> {
				val currentAggregate = buildAggregateFromHistory(
					aggregateId,
					aggregateClass,
					response.currentEvents.finalVersion,
					response.currentEvents,
					response.currentSnapshot
				)

				val newSnapshot = currentAggregate.getSnapshotMapper().toSnapshot(currentAggregate, messageFormat)
				val createSnapshotResponse = eventStore.saveEvents(
					AggregateIdentity(aggregateId, aggregateClass.simpleName, initialVersion),
					Events(aggregateId, finalVersion, events),
					SaveOptions(newSnapshot)
				)

				when (createSnapshotResponse) {
					is SaveEventsResponse.Success -> {
						try {
							eventPublisher.publish(events)
							aggregate.commitEvents()
						} catch (ex: PublishErrorException) {
							eventStore.revertToVersion(AggregateIdentity(aggregateId, aggregateClass.simpleName, initialVersion))
							throw ex
						}
					}
					is SaveEventsResponse.EventCollision -> {
						throw EventCollisionException(aggregateId, createSnapshotResponse.expectedVersion)
					}

					else -> throw IllegalStateException("Unable to save events.")
				}
			}

			else -> throw IllegalStateException("Unable to save events.")
		}
	}

	private fun <T : AggregateRoot> buildAggregateFromHistory(id: String, type: Class<T>, version: Long, events: Events, snapshot: Snapshot? = null): T {
		val adapter = AggregateAdapter<T>("apply")
		adapter.fetchMetadata(type)
		val history = mutableListOf<Event>()
		events.events.forEach {
			if (messageFormat.supportsKind(it.kind)) {
				val event = messageFormat.parse<Event>(it.eventData, it.kind)
				history.add(event)
			}
		}

		var aggregate: T
		try {
			aggregate = type.newInstance()
			if (snapshot != null) {
				aggregate = aggregate.fromSnapshot(snapshot.data, snapshot.version, messageFormat) as T

			}
		} catch (e: InstantiationException) {
			throw HydrationException(id, "target type: '${type.name}' cannot be instantiated")
		} catch (e: IllegalAccessException) {
			throw HydrationException(id, "target type: '${type.name}' has no default constructor")
		}
		aggregate.buildFromHistory(history, version)
		return aggregate
	}
}