package com.tsbonev.cqrs.core

import java.lang.RuntimeException

/**
 * @author Tsvetozar Bonev (tsbonev@gmail.com)
 */
class AggregateNotFoundException(aggregateId: String) : RuntimeException()

class EventCollisionException(aggregateId: String, val version: Long) : RuntimeException()

class PublishErrorException(val reason: Exception = Exception()) : RuntimeException()

class FailedValidationException(val errors: Map<String, List<String>>) : RuntimeException()

class HydrationException(val aggregateId: String) : RuntimeException()