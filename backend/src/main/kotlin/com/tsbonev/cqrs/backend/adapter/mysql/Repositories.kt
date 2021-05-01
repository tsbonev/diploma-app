package com.tsbonev.cqrs.backend.adapter.mysql

import com.google.gson.Gson
import com.tsbonev.cqrs.core.BinaryPayload
import com.tsbonev.cqrs.core.eventstore.AggregateIdentity
import com.tsbonev.cqrs.core.eventstore.CreationContext
import com.tsbonev.cqrs.core.eventstore.EventSourcedAggregate
import com.tsbonev.cqrs.core.eventstore.EventWithContext
import com.tsbonev.cqrs.core.eventstore.Events
import com.tsbonev.cqrs.core.snapshot.Snapshot
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository
import javax.persistence.Entity
import javax.persistence.Id
import javax.persistence.OneToMany
import javax.persistence.OneToOne
import javax.persistence.Table

@Repository
interface MysqlAggregateRepository : CrudRepository<AggregateEntity, String>

@Repository
interface MysqlEventRepository : CrudRepository<EventEntity, String>

@Repository
interface MysqlSnapshotRepository : CrudRepository<SnapshotEntity, String>

@Entity
@Table(name = "aggregates")
data class AggregateEntity(
	@Id val aggregateId: String,
	val type: String,
	val version: Long,
	@OneToMany(mappedBy = "aggregateId") val events: List<EventEntity>,
	@OneToOne val snapshot: SnapshotEntity?
) {
	companion object {
		fun fromEventSourcedAggregate(es: EventSourcedAggregate) : AggregateEntity {
			return AggregateEntity(
				es.aggregateIdentity.aggregateId,
				es.aggregateIdentity.aggregateType,
				es.aggregateIdentity.aggregateVersion,
				EventEntity.fromEvents(es.events),
				SnapshotEntity.fromSnapshot(es.aggregateIdentity.aggregateId, es.snapshot)
			)
		}
	}

	fun toEventSourcedAggregate() : EventSourcedAggregate {
		return EventSourcedAggregate(
			AggregateIdentity(this.aggregateId, this.type, this.version),
			Events(this.aggregateId, this.version, this.events.map { it.toEventWithContext() }),
			this.snapshot?.toSnapshot()
		)
	}
}


@Entity
@Table(name = "events")
data class EventEntity(
	@Id val eventId: String,
	val aggregateId: String,
	val kind: String,
	val data: ByteArray,
	val version: Long,
	val context: String
) {
	companion object {
		fun fromEvents(events: Events) : List<EventEntity> {
			return events.events.map { event ->
				EventEntity("${events.aggregateId}_${event.version}",
				            events.aggregateId, event.kind, event.eventData, event.version,
				            Gson().toJson(event.creationContext))
			}
		}
	}

	fun toEventWithContext() : EventWithContext {
		return EventWithContext(data, kind, version, Gson().fromJson(context, CreationContext::class.java))
	}
}

@Entity
@Table(name = "snapshots")
data class SnapshotEntity(
	@Id val aggregateId: String,
	val version: Long,
	val data: ByteArray
) {
	companion object {
		fun fromSnapshot(aggregateId: String, snapshot: Snapshot?) : SnapshotEntity? {
			if(snapshot == null) return null

			return SnapshotEntity(aggregateId, snapshot.version, snapshot.data.payload)
		}
	}

	fun toSnapshot() : Snapshot {
		return Snapshot(version, BinaryPayload(data))
	}
}