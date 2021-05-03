package com.tsbonev.cqrs.core

import java.lang.RuntimeException


class AggregateNotFoundException(val aggregateId: String) : RuntimeException()

class EventCollisionException(val aggregateId: String, val version: Long) : RuntimeException()

class PublishErrorException(val reason: Exception = Exception()) : RuntimeException()

class HydrationException(val aggregateId: String, override val message: String) : RuntimeException(message)