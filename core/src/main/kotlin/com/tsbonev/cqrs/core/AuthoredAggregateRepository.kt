package com.tsbonev.cqrs.core

/**
 * Aggregate Repository chained with Identity Provider to provide configurable identity system to aggregates.
 *
 * @author Tsvetozar Bonev (tsbonev@gmail.com)
 */
class AuthoredAggregateRepository(
	private val identityProvider: IdentityProvider,
	private val identityAggregateRepository: IdentityAggregateRepository
) : Aggregates {
	override fun <T : AggregateRoot> save(aggregate: T) {
		val identity = identityProvider.get()
		identityAggregateRepository.save(aggregate, identity)
	}


	override fun <T : AggregateRoot> getById(aggregateId: String, type: Class<T>): T {
		val identity = identityProvider.get()
		return identityAggregateRepository.getById(aggregateId, type, identity)
	}

	override fun <T : AggregateRoot> getByIds(ids: List<String>, type: Class<T>): Map<String, T> {
		val identity = identityProvider.get()
		return identityAggregateRepository.getByIds(ids, type, identity)
	}
}