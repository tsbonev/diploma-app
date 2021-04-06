package com.tsbonev.cqrs.core

/**
 * @author Tsvetozar Bonev (tsbonev@gmail.com)
 */
interface AggregateConfiguration {
	/**
	 * Determines the topic name of the AggregateRoot.
	 */
	fun topicName(aggregate: Aggregate): String
}