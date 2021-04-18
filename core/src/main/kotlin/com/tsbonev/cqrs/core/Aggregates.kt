package com.tsbonev.cqrs.core


interface Aggregates {
	/**
	 * Creates a new or updates an existing aggregate in the repository.
	 */
	@Throws(PublishErrorException::class, EventCollisionException::class)
	fun <T : AggregateRoot> save(aggregate: T)

	/**
	 * Get the aggregate
	 */
	@Throws(HydrationException::class, AggregateNotFoundException::class)
	fun <T : AggregateRoot> getById(aggregateId: String, type: Class<T>): T

	/**
	 * Get a set of aggregates by providing a list of ids.
	 */
	@Throws(HydrationException::class)
	fun <T : AggregateRoot> getByIds(ids: List<String>, type: Class<T>): Map<String, T>

}