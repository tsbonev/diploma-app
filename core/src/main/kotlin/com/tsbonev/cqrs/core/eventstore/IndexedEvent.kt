package com.tsbonev.cqrs.core.eventstore


data class IndexedEvent(val position: Position, val tenant: String, val aggregateType: String, val version: Long, val payload: EventPayload)