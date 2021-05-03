package com.tsbonev.cqrs.backend.adapter.mysql

import com.google.gson.Gson
import com.tsbonev.cqrs.core.BinaryPayload
import com.tsbonev.cqrs.core.eventstore.AggregateIdentity
import com.tsbonev.cqrs.core.eventstore.CreationContext
import com.tsbonev.cqrs.core.eventstore.EventSourcedAggregate
import com.tsbonev.cqrs.core.eventstore.EventWithContext
import com.tsbonev.cqrs.core.eventstore.Events
import com.tsbonev.cqrs.core.snapshot.Snapshot
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository
import java.lang.RuntimeException
import javax.persistence.CascadeType
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.FetchType
import javax.persistence.Id
import javax.persistence.JoinColumn
import javax.persistence.ManyToOne
import javax.persistence.OneToMany
import javax.persistence.OneToOne
import javax.persistence.Table

@Repository
interface MysqlAggregateRepository : JpaRepository<AggregateEntity, String>

@Repository
interface MysqlEventRepository : JpaRepository<EventEntity, String> {
	fun findByAggregateId(aggregateId: String): List<EventEntity>

	fun findByAggregateIdAndVersionGreaterThan(aggregateId: String, version: Long): List<EventEntity>
}

@Repository
interface MysqlSnapshotRepository : JpaRepository<SnapshotEntity, String>

@Entity
@Table(name = "aggregates")
data class AggregateEntity(
	@Id val aggregateId: String,
	val type: String,
	val version: Long
) {
	companion object {
		fun fromAggregateIdentity(identity: AggregateIdentity) : AggregateEntity {
			return AggregateEntity(
				identity.aggregateId,
				identity.aggregateType,
				identity.aggregateVersion
			)
		}
	}

	fun toAggregateIdentity() : AggregateIdentity {
		return AggregateIdentity(this.aggregateId, this.type, this.version)
	}
}


@Entity
@Table(name = "events")
data class EventEntity(
	@Id val eventId: String,
	val kind: String,
	val data: ByteArray,
	val version: Long,
	val context: String,
	val aggregateId: String
) {
	companion object {
		fun fromEvents(events: Events) : List<EventEntity> {
			return events.events.map { event ->
				EventEntity("${events.aggregateId}_${event.version}",
				            event.kind, event.eventData, event.version,
				            Gson().toJson(event.creationContext), events.aggregateId)
			}
		}
	}

	fun toEventWithContext() : EventWithContext {
		return EventWithContext(data, kind, version, Gson().fromJson(context, CreationContext::class.java))
	}
}

fun List<EventEntity>.toEvents(): Events {
	val firstEvent = this.maxByOrNull { it.version }
	return Events(firstEvent!!.aggregateId, firstEvent.version, this.map {
		it.toEventWithContext()
	} )
}

@Entity
@Table(name = "snapshots")
data class SnapshotEntity(
	@Id val snapshotId: String,
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

class EventsNotFoundException : RuntimeException()