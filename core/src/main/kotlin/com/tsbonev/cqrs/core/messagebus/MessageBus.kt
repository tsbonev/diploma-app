package com.tsbonev.cqrs.core.messagebus

import com.tsbonev.cqrs.core.EventWithBinaryPayload
import com.tsbonev.nharker.cqrs.Workflow
import java.lang.reflect.Method

/**
 * Provides the methods to send commands and register handlers that
 * listen for the events that they produce.
 *
 * @author Tsvetozar Bonev (tsbonev@gmail.com)
 */
interface MessageBus {
	/**
	 * Registers a workflow object to the event bus by
	 * looking up its annotated methods.
	 *
	 * @param workflow The workflow to register.
	 *
	 * @exception IllegalHandlerInWorkflowException thrown when a handler in the workflow
	 * does not meet the requirements for a handler.
	 * @exception NoHandlersInWorkflowException thrown when a workflow contains
	 * no handler annotations.
	 * @exception CommandAlreadyHandledException thrown when a command handler
	 * attempts handling a command that is already handled.
	 */
	@Throws(
		IllegalHandlerInWorkflowException::class,
		NoHandlersInWorkflowException::class,
		CommandAlreadyHandledException::class
	)
	fun registerWorkflow(workflow: Workflow)

	/**
	 * Registers an interceptor that will process events and commands before
	 * the event bus.
	 *
	 * @param interceptor The interceptor to register.
	 */
	fun registerInterceptor(interceptor: Interceptor)

	/**
	 * Executes a command by delegating it to the
	 * registered command handler.
	 *
	 * @param command The command to execute.
	 * @return The response of the command.
	 */
	fun <T : Command> send(command: T): CommandResponse

	/**
	 * Publishes an event by delegating it to all
	 * registered event handlers.
	 *
	 * @param event The event to publish.
	 */
	@Throws(Exception::class)
	fun publish(event: EventWithBinaryPayload)
}

/**
 * Invokers to store along with the workflow instance
 *
 * @param method The method to invoke.
 * @param instance The instance to invoke it with.
 */
class EventInvoker(private val method: Method, private val instance: Workflow) {
	fun invoke(event: Event) {
		method.invoke(instance, event)
	}
}

class CommandInvoker(private val method: Method, private val instance: Workflow) {
	fun invoke(command: Command): CommandResponse {
		return method.invoke(instance, command) as CommandResponse
	}
}

/**
 * Exceptions meant to fail in runtime if a workflow is incorrectly defined.
 *
 * @see EventHandler for requirements.
 * @see CommandHandler for requirements.
 */
class IllegalHandlerInWorkflowException(message: String) : Throwable(message)

class CommandAlreadyHandledException(message: String) : Throwable(message)
class NoHandlersInWorkflowException(message: String) : Throwable(message)
