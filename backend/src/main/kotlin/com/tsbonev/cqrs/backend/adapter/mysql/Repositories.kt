package com.tsbonev.cqrs.backend.adapter.mysql

import com.google.gson.Gson
import com.tsbonev.cqrs.core.eventstore.AggregateIdentity
import com.tsbonev.cqrs.core.eventstore.CreationContext
import com.tsbonev.cqrs.core.eventstore.EventWithContext
import com.tsbonev.cqrs.core.eventstore.Events
import com.tsbonev.cqrs.core.snapshot.Snapshot
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import javax.persistence.Entity
import javax.persistence.Id
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
	val data: String,
	val version: Long,
	val context: String,
	val aggregateId: String
) {
	companion object {
		fun fromEvents(events: Events) : List<EventEntity> {
			return events.events.map { event ->
				EventEntity("${events.aggregateId}_${event.version}",
				            event.kind, event.eventData as String, event.version,
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
	val data: String
) {
	companion object {
		fun fromSnapshot(aggregateId: String, snapshot: Snapshot?) : SnapshotEntity? {
			if(snapshot == null) return null

			return SnapshotEntity(aggregateId, snapshot.version, snapshot.data as String)
		}
	}

	fun toSnapshot() : Snapshot {
		return Snapshot(version, data)
	}
}