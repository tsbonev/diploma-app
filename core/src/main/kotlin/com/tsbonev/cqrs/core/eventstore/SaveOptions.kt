package com.tsbonev.cqrs.core.eventstore

import com.tsbonev.cqrs.core.snapshot.Snapshot
import java.util.UUID


data class SaveOptions(var aggregateId: String = "", val version: Long = 0L, val topicName: String = "", val createSnapshotRequest: CreateSnapshotRequest = CreateSnapshotRequest()) {
	init {
		if (aggregateId == "") {
			aggregateId = UUID.randomUUID().toString()
		}
	}
}

data class CreateSnapshotRequest(val required: Boolean = false, val snapshot: Snapshot? = null)