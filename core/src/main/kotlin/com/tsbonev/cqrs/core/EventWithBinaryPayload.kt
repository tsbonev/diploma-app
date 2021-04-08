package com.tsbonev.cqrs.core


data class EventWithBinaryPayload(val event: Any, val payload: BinaryPayload)

