package com.tsbonev.cqrs.core.messagebus

/**
 * An annotation marking a method as a command handler.
 *
 * CommandHandlers:
 * Must return CommandResponse,
 * Must have one and only one parameter of type Command.
 * Myst be annotated with @CommandHandler.
 *
 * @author Tsvetozar Bonev (tsbonev@gmail.com)
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
annotation class CommandHandler