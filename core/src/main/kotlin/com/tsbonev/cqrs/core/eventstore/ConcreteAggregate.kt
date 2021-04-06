package com.tsbonev.cqrs.core.eventstore

import com.tsbonev.cqrs.core.snapshot.Snapshot

/**
 * @author Tsvetozar Bonev (tsbonev@gmail.com)
 */
data class ConcreteAggregate(val aggregateType: String, val snapshot: Snapshot?, val version: Long, val events: List<EventPayload>)