package com.tsbonev.cqrs.core


interface CommandHandler<in T : Command<V>, V> {
	@Throws(EventCollisionException::class, HydrationException::class, AggregateNotFoundException::class)
	fun handle(command: T): V
}