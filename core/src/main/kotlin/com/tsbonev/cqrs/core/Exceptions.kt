package com.tsbonev.cqrs.core

import java.lang.RuntimeException


class AggregateNotFoundException(aggregateId: String) : RuntimeException()

class EventCollisionException(aggregateId: String, val version: Long) : RuntimeException()

class PublishErrorException(val reason: Exception = Exception()) : RuntimeException()

class ValidationException(val errors: Map<String, List<String>>) : RuntimeException()

class HydrationException(val aggregateId: String, override val message: String) : RuntimeException(message)