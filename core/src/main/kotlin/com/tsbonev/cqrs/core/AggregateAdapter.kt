package com.tsbonev.cqrs.core


class AggregateAdapter<T : AggregateRoot>(private val applyCallName: String) {

	private val supportedEventNameToType = mutableMapOf<String, String>()

	fun fetchMetadata(type: Class<T>) {
		val methods = type.declaredMethods
		methods.forEach { method ->
			if (method.name === applyCallName) {
				method.parameters.forEach {
					supportedEventNameToType[it.type.simpleName] = it.type.name
				}
			}
		}
	}

	fun eventType(eventName: String): String? {
		return supportedEventNameToType[eventName]
	}
}