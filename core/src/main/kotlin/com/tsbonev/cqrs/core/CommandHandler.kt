package com.tsbonev.cqrs.core

/**
 * @author Tsvetozar Bonev (tsbonev@gmail.com)
 */
interface CommandHandler<in T : Command<V>, V> {
	@Throws(EventCollisionException::class, HydrationException::class, AggregateNotFoundException::class)
	fun handle(command: T): V
}