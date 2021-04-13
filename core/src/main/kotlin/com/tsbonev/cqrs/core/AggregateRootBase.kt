package com.tsbonev.cqrs.core

import com.tsbonev.cqrs.core.messagebus.Event
import com.tsbonev.cqrs.core.snapshot.MessageFormat
import com.tsbonev.cqrs.core.snapshot.Snapshot
import com.tsbonev.cqrs.core.snapshot.SnapshotMapper
import java.io.ByteArrayInputStream
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method


abstract class AggregateRootBase private constructor(protected var aggregateId: String = "",
                                                     protected var version: Long = 0L): AggregateRoot {
	private val mutations: ArrayList<Event> = arrayListOf();

	constructor(): this("")

	override fun getId(): String {
		return aggregateId
	}

	override fun commitEvents() {
		mutations.clear()
	}

	override fun getEvents(): List<Event> {
		return mutations
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
			mutations.add(event)
		}
	}

	override fun getSnapshotMapper(): SnapshotMapper<AggregateRoot> {
		return object : SnapshotMapper<AggregateRoot> {
			override fun toSnapshot(data: AggregateRoot, messageFormat: MessageFormat): Snapshot {
				return Snapshot(data.getExpectedVersion(), BinaryPayload(messageFormat.formatToBytes(data)))
			}

			override fun fromSnapshot(
				snapshot: ByteArray,
				snapshotVersion: Long,
				messageFormat: MessageFormat
			): AggregateRoot {
				return messageFormat.parse(ByteArrayInputStream(snapshot), this@AggregateRootBase::class.java.simpleName)
			}
		}
	}

	@Suppress("UNCHECKED_CAST")
	final override fun <T : AggregateRoot> fromSnapshot(
		snapshotData: ByteArray,
		snapshotVersion: Long,
		messageFormat: MessageFormat
	): T {
		val snapshotRootBase = getSnapshotMapper().fromSnapshot(snapshotData,
		                                                        snapshotVersion,
		                                                        messageFormat) as AggregateRootBase
		snapshotRootBase.version = snapshotVersion
		return snapshotRootBase as T
	}
}