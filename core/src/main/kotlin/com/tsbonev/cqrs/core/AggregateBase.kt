package com.tsbonev.cqrs.core

import com.tsbonev.cqrs.core.snapshot.MessageFormat
import com.tsbonev.cqrs.core.snapshot.Snapshot
import com.tsbonev.cqrs.core.snapshot.SnapshotMapper
import java.io.ByteArrayInputStream
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method


abstract class AggregateBase private constructor(protected var aggregateId: String = "", protected var version: Long = 0L): Aggregate {
	private val mutations: ArrayList<Any> = arrayListOf();

	constructor(): this("")

	override fun getId(): String {
		return aggregateId
	}

	override fun commitEvents() {
		mutations.clear()
	}

	override fun getEvents(): List<Any> {
		return mutations
	}

	override fun getExpectedVersion(): Long {
		return version
	}

	override fun buildFromHistory(history: Iterable<Any>, version: Long) {
		this.version = version
		for (event in history) {
			mutateState(event, false)
		}
	}

	protected fun mutateState(event: Any, isNew: Boolean = true) {
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


	/**
	 * Sets up the state after a snapshot.
	 */
	private fun setupState(donor: Any, recipient: Any) {
		donor.javaClass.declaredFields.forEach { field ->
			try {
				val donorField = donor.javaClass.getDeclaredField(field.name)
				donorField.isAccessible = true
				val value = donorField.get(donor)
				val declaredField = recipient.javaClass.getDeclaredField(field.name)
				declaredField.isAccessible = true
				declaredField.set(recipient, value)
			} catch (e: IllegalAccessException) {
				throw IllegalStateException(e)
			} catch (e: NoSuchFieldException) {
				throw IllegalStateException(e)
			}
		}

		val aggregateIdFieldDonor = donor.javaClass.superclass.getDeclaredField("aggregateId")
		aggregateIdFieldDonor.isAccessible = true
		val value = aggregateIdFieldDonor.get(donor)
		val declaredField = recipient.javaClass.superclass.getDeclaredField("aggregateId")
		declaredField.isAccessible = true
		declaredField.set(recipient, value)
	}

	override fun getSnapshotMapper(): SnapshotMapper<Aggregate> {
		return object : SnapshotMapper<Aggregate> {
			override fun toSnapshot(data: Aggregate, messageFormat: MessageFormat): Snapshot {
				return Snapshot(data.getExpectedVersion(), BinaryPayload(messageFormat.formatToBytes(data)))
			}

			override fun fromSnapshot(
				snapshot: ByteArray,
				snapshotVersion: Long,
				messageFormat: MessageFormat
			): Aggregate {
				return messageFormat.parse(ByteArrayInputStream(snapshot), this@AggregateBase::class.java.simpleName)
			}
		}
	}

	@Suppress("UNCHECKED_CAST")
	final override fun <T : Aggregate> fromSnapshot(
		snapshotData: ByteArray,
		snapshotVersion: Long,
		messageFormat: MessageFormat
	): T {
		val snapshotRootBase = getSnapshotMapper().fromSnapshot(snapshotData, snapshotVersion, messageFormat)
		val newInstance = this@AggregateBase::class.java.newInstance()
		setupState(snapshotRootBase, newInstance)
		newInstance.version = snapshotVersion
		return newInstance as T
	}
}