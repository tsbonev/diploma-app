package com.tsbonev.cqrs.core.snapshot

import com.tsbonev.cqrs.core.Aggregate


interface SnapshotMapper<T : Aggregate> {

	/**
	 * Serializes the current entity to a string snapshot
	 */
	fun toSnapshot(data: T, messageFormat: MessageFormat): Snapshot

	/**
	 * Create an aggregate from given snapshot
	 */
	fun fromSnapshot(snapshot: ByteArray, snapshotVersion: Long, messageFormat: MessageFormat): T
}