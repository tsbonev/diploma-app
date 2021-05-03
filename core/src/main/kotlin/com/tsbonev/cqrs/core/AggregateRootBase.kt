package com.tsbonev.cqrs.core

import com.tsbonev.cqrs.core.messagebus.Event
import com.tsbonev.cqrs.core.snapshot.MessageFormat
import com.tsbonev.cqrs.core.snapshot.Snapshot
import com.tsbonev.cqrs.core.snapshot.SnapshotMapper
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method
import java.util.UUID


abstract class AggregateRootBase private constructor(
	protected var aggregateId: String = UUID.randomUUID().toString(),
	protected var version: Long = -1L
) : AggregateRoot {
	private val events: ArrayList<Event> = arrayListOf();

	constructor() : this("")

	override fun getId(): String {
		return aggregateId
	}

	override fun commitEvents() {
		events.clear()
	}

	override fun getEvents(): List<Event> {
		return events
	}

	override fun getExpectedVersion(): Long {
		return version
	}

	override fun buildFromHistory(history: Iterable<Event>, version: Long) {
		this.version = version
		for (event in history) {
			applyChange(event, false)
		}
	}

	protected fun applyChange(event: Event, isNew: Boolean = true) {
		if(isNew) { version++ }

		var method: Method? = null

		try {
			method = this::class.java.getDeclaredMethod("apply", event::class.java)
		} catch (e: NoSuchMethodException) {
			/**
			 * Events do not necessarily mutate the state.
			 */
		}

		if (method != null) {
			method.isAccessible = true
			try {
				method.invoke(this, event)
			} catch (e: IllegalAccessException) {
				throw IllegalStateException(e)
			} catch (e: IllegalArgumentException) {
				throw IllegalStateException(e)
			} catch (e: InvocationTargetException) {
				throw IllegalStateException(e)
			}
		}

		if (isNew) {
			events.add(event)
		}
	}

	override fun getSnapshotMapper(): SnapshotMapper<AggregateRoot> {
		return object : SnapshotMapper<AggregateRoot> {
			override fun toSnapshot(data: AggregateRoot, messageFormat: MessageFormat<Any>): Snapshot {
				return Snapshot(data.getExpectedVersion(), messageFormat.format(data))
			}

			override fun fromSnapshot(
				snapshot: Any,
				snapshotVersion: Long,
				messageFormat: MessageFormat<Any>
			): AggregateRoot {
				return messageFormat.parse(
					snapshot,
					this@AggregateRootBase::class.java.simpleName
				)
			}
		}
	}

	@Suppress("UNCHECKED_CAST")
	final override fun <T : AggregateRoot> fromSnapshot(snapshotData: Any, snapshotVersion: Long, messageFormat: MessageFormat<Any>): T {
		val snapshotRootBase = getSnapshotMapper().fromSnapshot(
			snapshotData,
			snapshotVersion,
			messageFormat
		) as AggregateRootBase
		snapshotRootBase.version = snapshotVersion
		return snapshotRootBase as T
	}
}