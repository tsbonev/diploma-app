package com.tsbonev.cqrs.core

import com.tsbonev.cqrs.core.messagebus.Event


data class EventWithBinaryPayload(val event: Event, val payload: BinaryPayload) : Event

