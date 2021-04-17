package com.tsbonev.cqrs.core.messagebus

import com.tsbonev.nharker.cqrs.Workflow
import org.slf4j.LoggerFactory
import java.lang.reflect.Method

/**
 * @author Tsvetozar Bonev (tsbonev@gmail.com)
 */
class SimpleMessageBus : MessageBus {
	private val logger = LoggerFactory.getLogger("SimpleMessageBus")

	/**
	 * Registered command handlers.
	 */
	private val commandHandlers = mutableMapOf<String, CommandInvoker>()

	/**
	 * Registered event handlers.
	 */
	private val eventHandlers = mutableMapOf<String, MutableList<EventInvoker>>()

	/**
	 * Registered interceptors.
	 */
	private val interceptors = mutableSetOf<Interceptor>()

	override fun registerWorkflow(workflow: Workflow) {
		val methods = workflow::class.java.declaredMethods

		val declaredCommandHandlers = mutableMapOf<String, Method>()
		val declaredEventHandlers = mutableMapOf<String, MutableList<Method>>()

		methods.forEach {
			val parameterTypes = it.parameterTypes

			/**
			 * Add the method as a command handler.
			 */
			if (it.isAnnotationPresent(CommandHandler::class.java)) {
				validateMethod(it)
				val handlerName = parameterTypes[0].name

				if (commandHandlers[handlerName] != null)
					throw CommandAlreadyHandledException(
						"${it.name} tries to publish a command that has" +
								" already been handled!"
					)

				declaredCommandHandlers[handlerName] = it
			}
			/**
			 * Add the method as an event handler.
			 */
			else if (it.isAnnotationPresent(EventHandler::class.java)) {
				validateMethod(it)
				val handlerName = parameterTypes[0].name

				if (declaredEventHandlers[handlerName] == null)
					declaredEventHandlers[handlerName] = mutableListOf()

				declaredEventHandlers[handlerName]!!.add(it)
			}
		}

		/**
		 * Workflow has not declared any event or command handlers.
		 */
		if (declaredCommandHandlers.isEmpty() && declaredEventHandlers.isEmpty())
			throw NoHandlersInWorkflowException(
				"${workflow::class.java.name} does not declare any" +
						" command or event handlers!"
			)

		declaredCommandHandlers.forEach {
			commandHandlers[it.key] = CommandInvoker(it.value, workflow)
		}

		declaredEventHandlers.forEach { (key, value) ->
			if (eventHandlers[key] == null) eventHandlers[key] = mutableListOf()

			value.forEach {
				eventHandlers[key]!!.add(EventInvoker(it, workflow))
			}
		}
	}

	override fun registerInterceptor(interceptor: Interceptor) {
		interceptors.add(interceptor)
	}

	override fun <T : Command> send(command: T): CommandResponse {
		val key = command::class.java.name

		val commandHandler = commandHandlers[key]

		return commandHandler?.invoke(command)
			?: throw NoHandlersInWorkflowException("No handler registered for $key command class")
	}

	override fun publish(event: Event) {
		val key = event::class.java.name

		val handlers = eventHandlers[key]

		if (handlers == null) {
			interceptors.forEach {
				it.intercept(SimpleChain(event, listOf()))
			}
			return logger.warn("No handlers registered for $key event class")
		}

		if (interceptors.isNotEmpty()) {
			interceptors.forEach {
				it.intercept(SimpleChain(event, handlers))
			}
			return
		}

		handlers.forEach {
			it.invoke(event)
		}
	}

	/**
	 * Validates a method's declared event and command handlers' signatures.
	 */
	private fun validateMethod(method: Method) {
		val parameterTypes = method.parameterTypes

		/**
		 * Method has no or too many parameters to be a handler.
		 */
		if (parameterTypes.isEmpty() || parameterTypes.count() > 1)
			throw IllegalHandlerInWorkflowException("${method.name} contains an invalid handler!")

		/**
		 * Method requires a non-event or non-command parameter when labeled as a command or event handler.
		 */
		if ((!Event::class.java.isAssignableFrom(parameterTypes[0])
					&& method.isAnnotationPresent(EventHandler::class.java))
			||
			(!Command::class.java.isAssignableFrom(parameterTypes[0])
					&& method.isAnnotationPresent(CommandHandler::class.java))
		)
			throw IllegalHandlerInWorkflowException("${method.name} contains an invalid handler!")

		/**
		 * Command handler does not return a CommandResponse.
		 */
		if (method.isAnnotationPresent(CommandHandler::class.java)
			&& !CommandResponse::class.java.isAssignableFrom(method.returnType)
		)
			throw IllegalHandlerInWorkflowException("${method.name} does not return a command response!")
	}
}