package com.tsbonev.cqrs.backend.domain

import com.tsbonev.cqrs.core.AggregateRootBase
import com.tsbonev.cqrs.core.messagebus.Command
import com.tsbonev.cqrs.core.messagebus.Event
import java.util.UUID

data class Product(var productId: String = UUID.randomUUID().toString(), var name: String) : AggregateRootBase() {

	init {
		applyChange(ProductCreatedEvent(productId, name))
	}

	fun apply(event: ProductCreatedEvent) {
		productId = event.productId
		name = event.productName
	}

}

data class ProductCreatedEvent(val productId: String, val productName: String) : Event
data class CreateProductCommand(val productId: String, val productName: String) : Command