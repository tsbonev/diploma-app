package com.tsbonev.cqrs.backend.adapter.mongodb

data class EventModel(
	@JvmField val aggregateId: String,
	@JvmField val kind: String,
	@JvmField val version: Long,
	@JvmField val identityId: String,
	@JvmField val timestamp: Long,
	@JvmField val payload: String
)