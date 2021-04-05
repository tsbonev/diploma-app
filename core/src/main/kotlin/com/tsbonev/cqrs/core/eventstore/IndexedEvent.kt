package com.tsbonev.cqrs.core.eventstore

/**
 * @author Tsvetozar Bonev (tsbonev@gmail.com)
 */
data class IndexedEvent(val position: Position, val tenant: String, val aggregateType: String, val version: Long, val payload: EventPayload)