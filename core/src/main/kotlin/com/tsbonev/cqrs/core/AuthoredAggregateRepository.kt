package com.tsbonev.cqrs.core

/**
 * Aggregate Repisotory chained with Identity Provider to provide configurable identity system to aggregates.
 *
 * @author Tsvetozar Bonev (tsbonev@gmail.com)
 */
sealed class AuthoredAggregateRepository(private val identityProvider: IdentityProvider,
                                         private val identityAggregateRepository: IdentityAggregateRepository
) : AggregateRepository {

	override fun <T : AggregateRoot> save(aggregate: T) {
		val identity = identityProvider.get()
		identityAggregateRepository.save(aggregate, identity)
	}

	override fun <T : AggregateRoot> save(stream: String, aggregate: T) {
		val identity = identityProvider.get()
		identityAggregateRepository.save(stream, aggregate, identity)
	}

	override fun <T : AggregateRoot> getById(id: String, type: Class<T>): T {
		val identity = identityProvider.get()
		return identityAggregateRepository.getById(id, type, identity)
	}

	override fun <T : AggregateRoot> getById(stream: String, aggregateId: String, type: Class<T>): T {
		val identity = identityProvider.get()
		return identityAggregateRepository.getById(stream, aggregateId, type, identity)
	}

	override fun <T : AggregateRoot> getByIds(ids: List<String>, type: Class<T>): Map<String, T> {
		val identity = identityProvider.get()
		return identityAggregateRepository.getByIds(ids, type, identity)
	}
}