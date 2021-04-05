package com.tsbonev.cqrs.core.messagebus

/**
 * @author Tsvetozar Bonev (tsbonev@gmail.com)
 */
interface Validator<in T> {
	fun validate(fieldValue: T): Boolean
}