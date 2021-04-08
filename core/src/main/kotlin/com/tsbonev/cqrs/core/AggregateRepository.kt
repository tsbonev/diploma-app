package com.tsbonev.cqrs.core


interface AggregateRepository {
	/**
	 * Creates a new or updates an existing aggregate in the repository.
	 */
	@Throws(PublishErrorException::class, EventCollisionException::class)
	fun <T : Aggregate> commit(aggregate: T)

	/**
	 * Creates a new or updates an existing aggregate in the repository.
	 */
	@Throws(PublishErrorException::class, EventCollisionException::class)
	fun <T : Aggregate> commit(stream: String, aggregate: T)

	/**
	 * Get the aggregate
	 */
	@Throws(HydrationException::class, AggregateNotFoundException::class)
	fun <T : Aggregate> getById(id: String, type: Class<T>): T

	/**
	 * Get the aggregate
	 */
	@Throws(HydrationException::class, AggregateNotFoundException::class)
	fun <T : Aggregate> getById(stream: String, aggregateId: String, type: Class<T>): T

	/**
	 * Get a set of aggregates by providing a list of ids.
	 */
	@Throws(HydrationException::class)
	fun <T : Aggregate> getByIds(ids: List<String>, type: Class<T>): Map<String, T>
}