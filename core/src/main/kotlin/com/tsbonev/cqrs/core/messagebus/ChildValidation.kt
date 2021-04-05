package com.tsbonev.cqrs.core.messagebus

/**
 * @author Tsvetozar Bonev (tsbonev@gmail.com)
 */
class ChildValidation<T> {
	var validations: MutableList<Pair<T.() -> Boolean, String>> = mutableListOf()

	fun be(validate: T.() -> Boolean) = validate

	infix fun (T.() -> Boolean).not(error: String) {
		validations.add(this to error)
	}

}