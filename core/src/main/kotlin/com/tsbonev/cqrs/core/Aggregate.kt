package com.tsbonev.cqrs.core

import com.tsbonev.cqrs.core.snapshot.MessageFormat
import com.tsbonev.cqrs.core.snapshot.SnapshotMapper



/**
 * Aggregate interface
 */
interface Aggregate {

	/**
	 * Returns the Id of the Aggregate
	 *
	 * @return Aggregate Id
	 */
	fun getId(): String

	fun commitEvents()

	fun getEvents(): List<Any>

	/**
	 * Builds the state of an aggregate from a given history
	 *
	 * @param history
	 * @param version the version of the aggregate
	 * @throws HydrationException
	 */
	fun buildFromHistory(history: Iterable<Any>, version: Long)

	/**
	 * Returns the version of the aggregate when it was hydrated
	 * @return
	 */
	fun getExpectedVersion(): Long

	/**
	 * Returns a SnapshotMapper that will be used in creation
	 * of Snapshots for the EventStore
	 */
	fun getSnapshotMapper(): SnapshotMapper<Aggregate>

	/**
	 * Builds an aggregate from snapshot data and the current version of the snapshot
	 */
	fun <T : Aggregate> fromSnapshot(snapshotData: ByteArray, snapshotVersion: Long, messageFormat: MessageFormat): T
}