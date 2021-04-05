package com.tsbonev.cqrs.core.eventstore

import com.tsbonev.cqrs.core.BinaryPayload

/**
 * @author Tsvetozar Bonev (tsbonev@gmail.com)
 */
data class EventPayload(val aggregateId: String, val kind: String, val timestamp: Long, val identityId: String, val data: BinaryPayload) {
	constructor(kind: String, payload: String) : this("", kind, 0, "", BinaryPayload(payload.toByteArray(Charsets.UTF_8)))
}