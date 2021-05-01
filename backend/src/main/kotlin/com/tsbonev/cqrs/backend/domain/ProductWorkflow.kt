package com.tsbonev.cqrs.backend.domain

import com.tsbonev.cqrs.core.AggregateNotFoundException
import com.tsbonev.cqrs.core.Aggregates
import com.tsbonev.cqrs.core.messagebus.CommandHandler
import com.tsbonev.cqrs.core.messagebus.CommandResponse
import com.tsbonev.cqrs.core.messagebus.EventHandler
import com.tsbonev.cqrs.core.messagebus.StatusCode
import com.tsbonev.nharker.cqrs.Workflow
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import java.util.UUID

class ProductWorkflow(private val aggregateRepo: Aggregates) : Workflow {

	@CommandHandler
	fun handle(command: CreateProductCommand) : CommandResponse {
		val aggregateId = UUID.randomUUID().toString()

		val aggregate = try {
			aggregateRepo.getById(aggregateId, Product::class.java)
		} catch (e: AggregateNotFoundException) {
			Product(productId = command.productId, name = command.productName)
		}

		aggregateRepo.save(aggregate)

		return CommandResponse(StatusCode.Created, ProductCreatedCommandResponse(aggregate.getId(), aggregate.productId, aggregate.name))
	}

	@EventHandler
	fun handle(event: ProductCreatedEvent) {

	}

}

data class ProductCreatedCommandResponse(val aggregateId: String, val productId: String, val productName: String)