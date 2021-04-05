package com.tsbonev.cqrs.core

/**
 * @author Tsvetozar Bonev (tsbonev@gmail.com)
 */
interface Aggregates {

	@Throws(PublishErrorException::class, EventCollisionException::class)
	fun <T : Aggregate> save(aggregate: T)

	@Throws(PublishErrorException::class, EventCollisionException::class)
	fun <T : Aggregate> save(stream: String, aggregate: T)

	@Throws(HydrationException::class, AggregateNotFoundException::class)
	fun <T : Aggregate> get(id: String, type: Class<T>): T

	@Throws(HydrationException::class, AggregateNotFoundException::class)
	fun <T : Aggregate> get(stream: String, aggregateId: String, type: Class<T>): T

	@Throws(HydrationException::class)
	fun <T : Aggregates> get(ids: List<String>, type: Class<T>): Map<String, T>
}