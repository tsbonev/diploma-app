package com.tsbonev.cqrs.backend.domain

import com.tsbonev.cqrs.core.AggregateRootBase
import com.tsbonev.cqrs.core.messagebus.Command
import com.tsbonev.cqrs.core.messagebus.Event

data class Product(var name: String) : AggregateRootBase() {
	constructor() : this("")

	constructor(id: String, name: String) : this(name) {
		applyChange(ProductCreatedEvent(id, name))
	}

	fun changeName(name: String) {
		applyChange(ProductNameChangedEvent(getId(), name))
	}

	fun apply(event: ProductCreatedEvent) {
		aggregateId = event.productId
		name = event.productName
	}

	fun apply(event: ProductNameChangedEvent) {
		name = event.productName
	}
}

data class ProductCreatedEvent(val productId: String, val productName: String) : Event
data class ProductNameChangedEvent(val productId: String, val productName: String) : Event
data class CreateProductCommand(val productId: String, val productName: String) : Command
data class ChangeProductNameCommand(val productId: String, val productName: String) : Command