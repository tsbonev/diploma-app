package com.tsbonev.cqrs.core


interface IdentityAggregateRepository {

	/**
	 * Creates a new or updates an existing aggregate in the repository.
	 */
	@Throws(PublishErrorException::class, EventCollisionException::class)
	fun <T : Aggregate> commit(aggregate: T, identity: Identity)

	/**
	 * Creates a new or updates an existing aggregate in the repository.
	 */
	@Throws(PublishErrorException::class, EventCollisionException::class)
	fun <T : Aggregate> commit(stream: String, aggregate: T, identity: Identity)

	/**
	 * Get the aggregate
	 */
	@Throws(HydrationException::class, AggregateNotFoundException::class)
	fun <T : Aggregate> getById(id: String, type: Class<T>, identity: Identity): T


	/**
	 * Get the aggregate
	 */
	@Throws(HydrationException::class, AggregateNotFoundException::class)
	fun <T : Aggregate> getById(stream: String, aggregateId: String, type: Class<T>, identity: Identity): T

	/**
	 * Get a set of aggregates by providing a list of ids.
	 */
	@Throws(HydrationException::class)
	fun <T : Aggregate> getByIds(ids: List<String>, type: Class<T>, identity: Identity): Map<String, T>

}