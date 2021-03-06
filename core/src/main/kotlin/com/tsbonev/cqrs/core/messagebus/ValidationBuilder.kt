package com.tsbonev.cqrs.core.messagebus


class ValidationBuilder<T> {
	var childValidations: MutableMap<String, ChildValidation<T>> = mutableMapOf()

	operator fun String.invoke(init: ChildValidation<T>.() -> Unit) {
		childValidations[this] = ChildValidation<T>().apply(init)
	}

	fun build(): Validation<T> {
		return Validation(childValidations)
	}

}