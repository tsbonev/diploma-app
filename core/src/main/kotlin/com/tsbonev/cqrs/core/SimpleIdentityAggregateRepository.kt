package com.tsbonev.cqrs.core

import com.tsbonev.cqrs.core.eventstore.CreateSnapshotRequest
import com.tsbonev.cqrs.core.eventstore.EventPayload
import com.tsbonev.cqrs.core.eventstore.EventPublisher
import com.tsbonev.cqrs.core.eventstore.EventStore
import com.tsbonev.cqrs.core.eventstore.GetEventsFromStreamsRequest
import com.tsbonev.cqrs.core.eventstore.GetEventsResponse
import com.tsbonev.cqrs.core.eventstore.SaveEventsRequest
import com.tsbonev.cqrs.core.eventstore.SaveEventsResponse
import com.tsbonev.cqrs.core.eventstore.SaveOptions
import com.tsbonev.cqrs.core.messagebus.Event
import com.tsbonev.cqrs.core.snapshot.MessageFormat
import com.tsbonev.cqrs.core.snapshot.Snapshot
import java.io.ByteArrayInputStream
import java.util.UUID


class SimpleIdentityAggregateRepository(
	private val eventStore: EventStore,
	private val messageFormat: MessageFormat,
	private val eventPublisher: EventPublisher,
	private val configuration: AggregateConfiguration
) : IdentityAggregateRepository {

	override fun <T : AggregateRoot> save(stream: String, aggregate: T, identity: Identity) {
		var aggregateId = aggregate.getId()
		if (aggregateId == "") {
			aggregateId = UUID.randomUUID().toString()
		}

		val uncommittedEvents = aggregate.getEvents()

		val eventsWithPayload = uncommittedEvents.map {
			EventWithBinaryPayload(it, BinaryPayload(messageFormat.formatToBytes(it)))
		}

		val events = eventsWithPayload.map {
			EventPayload(
				aggregateId,
				it.event::class.java.simpleName,
				identity.time.toEpochMilli(),
				identity.id,
				it.payload
			)
		}

		/**
		 * If there are no news changes, the aggregate doesn't need updating.
		 */
		if (events.isEmpty()) return

		val aggregateClass = aggregate::class.java
		val aggregateType = aggregateClass.simpleName

		val topicName = configuration.topicName(aggregate)
		val saveEventsRequest = SaveEventsRequest(identity.tenant, stream, aggregateType, events)
		val response = eventStore.saveEvents(
			saveEventsRequest,
			SaveOptions(version = aggregate.getExpectedVersion(), topicName = topicName)
		)

		when (response) {
			is SaveEventsResponse.Success -> {
				try {
					eventPublisher.publish(eventsWithPayload)
					aggregate.commitEvents()
				} catch (ex: PublishErrorException) {
					eventStore.revertEvents(identity.tenant, stream, events.size)
					throw ex
				}
			}

			is SaveEventsResponse.EventCollision -> {
				throw EventCollisionException(aggregate.getId(), response.expectedVersion)
			}

			is SaveEventsResponse.SnapshotRequired -> {
				val currentAggregate = buildAggregateFromHistory(
					aggregateClass,
					response.currentEvents,
					response.version,
					aggregate.getId(),
					response.currentSnapshot
				)

				val newSnapshot = currentAggregate.getSnapshotMapper().toSnapshot(currentAggregate, messageFormat)
				val createSnapshotResponse = eventStore.saveEvents(
					saveEventsRequest,
					SaveOptions(
						version = newSnapshot.version,
						createSnapshotRequest = CreateSnapshotRequest(true, newSnapshot)
					)
				)

				when (createSnapshotResponse) {
					is SaveEventsResponse.Success -> {
						try {
							eventPublisher.publish(eventsWithPayload)
							aggregate.commitEvents()
						} catch (ex: PublishErrorException) {
							eventStore.revertEvents(aggregateType, aggregate.getId(), events.size)
							throw ex
						}
					}
					is SaveEventsResponse.EventCollision -> {
						throw EventCollisionException(aggregate.getId(), createSnapshotResponse.expectedVersion)
					}

					else -> throw IllegalStateException("Unable to save events.")
				}
			}

			else -> throw IllegalStateException("Unable to save events.")
		}
	}

	/**
	 * Creates a new or updates an existing aggregate in the repository.
	 *
	 * This method ensures a one to one relationship of stream and aggregate and will use
	 * AggregateType_AggregateId pattern for the name of the stream where data will be written.
	 *
	 * @param aggregate the aggregate to be registered
	 * @throws EventCollisionException is thrown in case of
	 */
	@Throws(EventCollisionException::class, PublishErrorException::class)
	override fun <T : AggregateRoot> save(aggregate: T, identity: Identity) {
		val aggregateClass = aggregate::class.java
		val aggregateType = aggregateClass.simpleName
		var aggregateId = aggregate.getId()

		if (aggregateId == "") {
			aggregateId = UUID.randomUUID().toString()
		}

		return this.save(StreamKey.of(aggregateType, aggregateId), aggregate, identity)
	}

	override fun <T : AggregateRoot> getById(stream: String, aggregateId: String, type: Class<T>, identity: Identity): T {
		when (val response = eventStore.getEventsFromStreams(
			GetEventsFromStreamsRequest(identity.tenant, listOf(stream)))
			) {
			is GetEventsResponse.Success -> {
				if (response.aggregates.isEmpty()) {
					throw AggregateNotFoundException(aggregateId)
				}

				val aggregate = response.aggregates.find { it.events[0].aggregateId == aggregateId }
					?: throw AggregateNotFoundException(aggregateId)

				//we are sure that only one aggregate will be returned
				return buildAggregateFromHistory(
					type,
					aggregate.events,
					aggregate.version,
					aggregateId,
					response.aggregates.first().snapshot
				)
			}
			else -> throw IllegalStateException("unknown state")
		}
	}

	override fun <T : AggregateRoot> getById(id: String, type: Class<T>, identity: Identity): T {
		val stream = StreamKey.of(type.simpleName, id)

		when (val response = eventStore.getEventsFromStreams(
			GetEventsFromStreamsRequest(identity.tenant, listOf(stream)))
			) {
			is GetEventsResponse.Success -> {
				if (response.aggregates.isEmpty()) {
					throw AggregateNotFoundException(id)
				}
				val aggregate = response.aggregates.first()

				return buildAggregateFromHistory(
					type,
					aggregate.events,
					aggregate.version,
					id,
					response.aggregates.first().snapshot
				)
			}
			else -> throw IllegalStateException("Unknown state")
		}
	}

	override fun <T : AggregateRoot> getByIds(ids: List<String>, type: Class<T>, identity: Identity): Map<String, T> {
		val aggregateType = type.simpleName
		if (ids.isEmpty()) {
			return mapOf()
		}

		val streamIds = ids.map { StreamKey.of(aggregateType, it) }

		when (val response = eventStore.getEventsFromStreams(
			GetEventsFromStreamsRequest(identity.tenant, streamIds))
			) {
			is GetEventsResponse.Success -> {
				val result = mutableMapOf<String, T>()
				response.aggregates.forEach {
					val aggregateId = it.events[0].aggregateId
					result[aggregateId] = buildAggregateFromHistory(
						type,
						it.events,
						it.version,
						aggregateId,
						it.snapshot
					)
				}
				return result
			}
			else -> throw IllegalStateException("Unknown state")
		}
	}

	private fun <T : AggregateRoot> buildAggregateFromHistory(type: Class<T>, events: List<EventPayload>, version: Long, id: String, snapshot: Snapshot? = null): T {
		val adapter = AggregateAdapter<T>("apply")
		adapter.fetchMetadata(type)
		val history = mutableListOf<Event>()
		events.forEach {
			if (messageFormat.supportsKind(it.kind)) {
				val event = messageFormat.parse<Any>(ByteArrayInputStream(it.data.payload), it.kind) as Event
				history.add(event)
			}
		}

		/*
		 * Create a new instance of the aggregate
		 */
		var aggregate: T
		try {
			aggregate = type.newInstance()
			if (snapshot != null) {
				aggregate = aggregate.fromSnapshot(snapshot.data.payload, snapshot.version, messageFormat) as T
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