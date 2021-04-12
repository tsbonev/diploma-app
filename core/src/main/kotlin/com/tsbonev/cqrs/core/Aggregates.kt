package com.tsbonev.cqrs.core


interface Aggregates {

	@Throws(PublishErrorException::class, EventCollisionException::class)
	fun <T : AggregateRoot> save(aggregate: T)

	@Throws(PublishErrorException::class, EventCollisionException::class)
	fun <T : AggregateRoot> save(stream: String, aggregate: T)

	@Throws(HydrationException::class, AggregateNotFoundException::class)
	fun <T : AggregateRoot> get(id: String, type: Class<T>): T

	@Throws(HydrationException::class, AggregateNotFoundException::class)
	fun <T : AggregateRoot> get(stream: String, aggregateId: String, type: Class<T>): T

	@Throws(HydrationException::class)
	fun <T : Aggregates> get(ids: List<String>, type: Class<T>): Map<String, T>
}