package com.tsbonev.cqrs.backend.domain

import com.tsbonev.cqrs.core.AggregateRootBase
import com.tsbonev.cqrs.core.messagebus.Command
import com.tsbonev.cqrs.core.messagebus.Event

data class Product(var name: String, var numberValue: Long) : AggregateRootBase() {
	constructor() : this("", 0L)

	constructor(id: String, name: String) : this(name, 0L) {
		applyChange(ProductCreatedEvent(id, name))
	}

	fun changeName(name: String) {
		applyChange(ProductNameChangedEvent(getId(), name))
	}

	fun addOneToNumValue() {
		applyChange(ProductNumberChangedEvent(getId(), numberValue + 1))
	}

	fun apply(event: ProductCreatedEvent) {
		aggregateId = event.productId
		name = event.productName
	}

	fun apply(event: ProductNameChangedEvent) {
		name = event.productName
	}

	fun apply(event: ProductNumberChangedEvent) {
		numberValue += 1
	}
}

data class ProductCreatedEvent(val productId: String, val productName: String) : Event
data class CreateProductCommand(val productId: String, val productName: String) : Command

data class ProductNameChangedEvent(val productId: String, val productName: String) : Event
data class ChangeProductNameCommand(val productId: String, val productName: String) : Command

data class ProductNumberChangedEvent(val productId: String, val number: Long): Event
data class ChangeProductNumberCommand(val productId: String): Command
