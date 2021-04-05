package com.tsbonev.cqrs.core

/**
 * @author Tsvetozar Bonev (tsbonev@gmail.com)
 */
interface EventHandler<in T> {
	fun handle(event: T)
}