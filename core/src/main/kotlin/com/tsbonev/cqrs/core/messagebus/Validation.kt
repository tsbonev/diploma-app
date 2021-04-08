package com.tsbonev.cqrs.core.messagebus


class Validation<in T>(private val validations: Map<String, ChildValidation<T>>) {
	companion object {
		operator fun <T> invoke(init: ValidationBuilder<T>.() -> Unit): Validation<T> {
			val builder = ValidationBuilder<T>()
			return builder.apply(init).build()
		}
	}

	fun validate(value: T): Map<String, List<String>> {
		val messages = mutableMapOf<String, List<String>>()
		validations.forEach { map ->
			val errors = map.value.validations.filter { !it.first.invoke(value) }.map { it.second }.takeIf { it.isNotEmpty() }
			errors?.also {
				messages[map.key] = it
			}
		}
		return messages
	}
}