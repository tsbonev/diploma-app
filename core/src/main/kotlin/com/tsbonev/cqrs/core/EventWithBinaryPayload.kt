package com.tsbonev.cqrs.core

/**
 * @author Tsvetozar Bonev (tsbonev@gmail.com)
 */
data class EventWithBinaryPayload(val event: Any, val payload: BinaryPayload)

