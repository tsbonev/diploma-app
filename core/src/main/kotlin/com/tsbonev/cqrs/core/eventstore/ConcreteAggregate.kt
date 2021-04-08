package com.tsbonev.cqrs.core.eventstore

import com.tsbonev.cqrs.core.snapshot.Snapshot


data class ConcreteAggregate(val aggregateType: String, val snapshot: Snapshot?, val version: Long, val events: List<EventPayload>)