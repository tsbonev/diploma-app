package com.tsbonev.cqrs.core

/**
 * @author Tsvetozar Bonev (tsbonev@gmail.com)
 */
object StreamKey {
	fun of(aggregateType: String, aggregateId: String) = "${aggregateType}_$aggregateId"
}