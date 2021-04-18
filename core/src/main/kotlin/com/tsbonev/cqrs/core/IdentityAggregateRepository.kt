package com.tsbonev.cqrs.core


interface IdentityAggregateRepository {

	/**
	 * Creates a new or updates an existing aggregate in the repository.
	 */
	@Throws(PublishErrorException::class, EventCollisionException::class)
	fun <T : AggregateRoot> save(aggregate: T, identity: Identity)

	/**
	 * Get the aggregate
	 */
	@Throws(HydrationException::class, AggregateNotFoundException::class)
	fun <T : AggregateRoot> getById(aggregateId: String, type: Class<T>, identity: Identity): T

	/**
	 * Get a set of aggregates by providing a list of ids.
	 */
	@Throws(HydrationException::class)
	fun <T : AggregateRoot> getByIds(ids: List<String>, type: Class<T>, identity: Identity): Map<String, T>

}