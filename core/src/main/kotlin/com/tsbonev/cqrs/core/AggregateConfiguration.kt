package com.tsbonev.cqrs.core


interface AggregateConfiguration {
	/**
	 * Determines the topic name of the AggregateRoot.
	 */
	fun topicName(aggregate: AggregateRoot): String
}