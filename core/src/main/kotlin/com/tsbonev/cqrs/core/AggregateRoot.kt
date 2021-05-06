package com.tsbonev.cqrs.core

import com.tsbonev.cqrs.core.messagebus.Event
import com.tsbonev.cqrs.core.snapshot.MessageFormat
import com.tsbonev.cqrs.core.snapshot.SnapshotMapper



/**
 * Aggregate interface
 */
interface AggregateRoot {

	/**
	 * Returns the Id of the Aggregate
	 *
	 * @return Aggregate Id
	 */
	fun getId(): String

	/**
	 * Clears the events stored to be applied.
	 */
	fun commitEvents()

	/**
	 * Returns a list of events to be applied.
	 */
	fun getEvents(): List<Event>

	/**
	 * Builds the state of an aggregate from a given history.
	 */
	fun buildFromHistory(history: Iterable<Event>, version: Long)

	/**
	 * Returns the version of the aggregate.
	 */
	fun getExpectedVersion(): Long

	/**
	 * Returns a SnapshotMapper that will be used in creation
	 * of Snapshots for the EventStore.
	 */
	fun getSnapshotMapper(): SnapshotMapper<AggregateRoot>

	/**
	 * Builds an aggregate from snapshot data and the current version of the snapshot.
	 */
	fun <T : AggregateRoot> fromSnapshot(snapshotData: Any, snapshotVersion: Long, messageFormat: MessageFormat<Any>): T
}