package com.tsbonev.cqrs.core.messagebus

import com.tsbonev.cqrs.core.AggregateNotFoundException
import com.tsbonev.cqrs.core.Command
import com.tsbonev.cqrs.core.CommandHandler
import com.tsbonev.cqrs.core.Event
import com.tsbonev.cqrs.core.EventCollisionException
import com.tsbonev.cqrs.core.EventHandler
import com.tsbonev.cqrs.core.EventWithBinaryPayload
import com.tsbonev.cqrs.core.HydrationException


interface MessageBus {

	fun <T : Command<R>, R> registerCommandHandler(aClass: Class<T>, handler: CommandHandler<T, R>, validation: Validation<T> = Validation {})

	/**
	 * Register an event handler
	 *
	 * @param aClass
	 * @param handler
	 */
	fun <T : Event> registerEventHandler(aClass: Class<T>, handler: EventHandler<T>)

	/**
	 * Register an interceptor through which all events will be intercepted.
	 */
	fun registerInterceptor(interceptor: Interceptor)

	/**
	 * Execute a command
	 *
	 * @param command
	 * @throws EventCollisionException
	 * @throws HydrationException
	 * @throws AggregateNotFoundException
	 */
	@Throws(EventCollisionException::class, HydrationException::class, AggregateNotFoundException::class)
	fun <T : Command<R>, R> send(command: T): R

	/**
	 * Handles event using the registered event handlers.
	 *
	 * @throws Exception different exceptions could be thrown when trying to handle the event.
	 */
	@Throws(Exception::class)
	fun handle(event: EventWithBinaryPayload)

}