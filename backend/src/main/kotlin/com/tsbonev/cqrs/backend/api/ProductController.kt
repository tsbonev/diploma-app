package com.tsbonev.cqrs.backend.api

import com.tsbonev.cqrs.backend.domain.ChangeProductNumberCommand
import com.tsbonev.cqrs.backend.domain.ChangeProductNameCommand
import com.tsbonev.cqrs.backend.domain.CreateProductCommand
import com.tsbonev.cqrs.backend.domain.ProductCreatedCommandResponse
import com.tsbonev.cqrs.backend.domain.ProductNameChangedCommandResponse
import com.tsbonev.cqrs.backend.domain.ProductNumberChangedCommandResponse
import com.tsbonev.cqrs.backend.view.mysql.MysqlProductView
import com.tsbonev.cqrs.core.messagebus.MessageBus
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.lang.RuntimeException
import java.util.UUID


@RestController
@RequestMapping("/products")
class ProductController(
	@Autowired val messageBus: MessageBus,
	@Autowired val productView: MysqlProductView
) {

	@PostMapping
	fun createProduct(@RequestBody createProductRequest: CreateProductRequest): ProductCreatedResponse {
		val createProductCommand = CreateProductCommand(UUID.randomUUID().toString(), createProductRequest.name)
		val commandResponse = messageBus.send(createProductCommand).payload

		if(commandResponse.isPresent) return ProductCreatedResponse(commandResponse.get() as ProductCreatedCommandResponse)
		else throw MessageBusException()
	}

	@PutMapping
	fun changeProductName(@RequestBody changeProductNameRequest: ChangeProductNameRequest): ProductNameChangedResponse {
		val createProductCommand = ChangeProductNameCommand(changeProductNameRequest.productId, changeProductNameRequest.name)
		val commandResponse = messageBus.send(createProductCommand).payload

		if(commandResponse.isPresent) return ProductNameChangedResponse(commandResponse.get() as ProductNameChangedCommandResponse)
		else throw MessageBusException()
	}

	@PutMapping("/number")
	fun addToNumberValue(@RequestBody changeProductNumberRequest: ChangeProductNumberRequest): ProductNumberChangedResponse {
		val createProductCommand = ChangeProductNumberCommand(changeProductNumberRequest.productId)
		val commandResponse = messageBus.send(createProductCommand).payload

		if(commandResponse.isPresent) return ProductNumberChangedResponse(commandResponse.get() as ProductNumberChangedCommandResponse)
		else throw MessageBusException()
	}

	@GetMapping
	fun getAllProducts(): ProductsResponse {
		val products = productView.findAll()

		return ProductsResponse(products.map{ ProductDto(it.productId, it.productName) })
	}
}

class MessageBusException : RuntimeException()

data class CreateProductRequest(val name: String)
data class ProductCreatedResponse(val id: String, val name: String) {
	constructor(response: ProductCreatedCommandResponse) : this(response.productId, response.productName)
}

data class ChangeProductNameRequest(val productId: String, val name: String)
data class ProductNameChangedResponse(val id: String, val name: String) {
	constructor(response: ProductNameChangedCommandResponse) : this(response.productId, response.productName)
}

data class ChangeProductNumberRequest(val productId: String)
data class ProductNumberChangedResponse(val id: String, val number: Long) {
	constructor(response: ProductNumberChangedCommandResponse) : this(response.productId, response.number)
}

data class ProductsResponse(val products: List<ProductDto>)
data class ProductDto(val productId: String, val name: String)
