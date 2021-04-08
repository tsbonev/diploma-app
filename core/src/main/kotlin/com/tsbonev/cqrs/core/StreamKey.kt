package com.tsbonev.cqrs.core


object StreamKey {
	fun of(aggregateType: String, aggregateId: String) = "${aggregateType}_$aggregateId"
}