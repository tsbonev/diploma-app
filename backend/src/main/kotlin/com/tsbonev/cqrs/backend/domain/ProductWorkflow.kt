package com.tsbonev.cqrs.backend.domain

import com.tsbonev.cqrs.backend.view.mysql.MysqlProductView
import com.tsbonev.cqrs.backend.view.mysql.ProductViewEntity
import com.tsbonev.cqrs.core.AggregateNotFoundException
import com.tsbonev.cqrs.core.Aggregates
import com.tsbonev.cqrs.core.messagebus.CommandHandler
import com.tsbonev.cqrs.core.messagebus.CommandResponse
import com.tsbonev.cqrs.core.messagebus.EventHandler
import com.tsbonev.cqrs.core.messagebus.StatusCode
import com.tsbonev.cqrs.core.messagebus.Workflow
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import java.lang.RuntimeException
import java.util.UUID

class ProductCommandsWorkflow(private val aggregateRepo: Aggregates) : Workflow {

	@CommandHandler
	fun handle(command: CreateProductCommand) : CommandResponse {
		val aggregate = try {
			aggregateRepo.getById(command.productId, Product::class.java)
		} catch (e: AggregateNotFoundException) {
			Product(command.productId, command.productName)
		}

		aggregateRepo.save(aggregate)

		return CommandResponse(StatusCode.Created, ProductCreatedCommandResponse(command.productId, aggregate.name))
	}

	@CommandHandler
	fun handle(command: ChangeProductNameCommand) : CommandResponse {
		val aggregate = try {
			aggregateRepo.getById(command.productId, Product::class.java)
		} catch (e: AggregateNotFoundException) {
			throw ProductNotFoundException(command.productId)
		}

		aggregate.changeName(command.productName)
		aggregateRepo.save(aggregate)
		return CommandResponse(StatusCode.Created, ProductNameChangedCommandResponse(command.productId, aggregate.name))
	}
}

@Component
class ProductEventsWorkflow(@Autowired private val view: MysqlProductView) : Workflow {

	@EventHandler
	fun handle(event: ProductCreatedEvent) {
		view.save(ProductViewEntity(event.productId, event.productName))
	}

	@EventHandler
	fun handle(event: ProductNameChangedEvent) {
		val product = view.findById(event.productId).orElseThrow { throw ProductNotFoundException(event.productId) }
		view.save(product.copy(productName = event.productName))
	}
}


data class ProductCreatedCommandResponse(val productId: String, val productName: String)
data class ProductNameChangedCommandResponse(val productId: String, val productName: String)
class ProductNotFoundException(val productId: String) : RuntimeException()