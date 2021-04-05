package com.tsbonev.cqrs.core.eventstore

/**
 * @author Tsvetozar Bonev (tsbonev@gmail.com)
 */
data class ConcreteAggregate(val aggregateType: String, val snapshot: Snapshot?, val version: Long, val events: List<EventPayload>)