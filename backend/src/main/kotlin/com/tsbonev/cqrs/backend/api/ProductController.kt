package com.tsbonev.cqrs.backend.api

import com.tsbonev.cqrs.backend.domain.CreateProductCommand
import com.tsbonev.cqrs.backend.domain.ProductCreatedCommandResponse
import com.tsbonev.cqrs.core.messagebus.MessageBus
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.lang.RuntimeException
import java.time.Instant
import java.util.UUID


@RestController
@RequestMapping("/products")
class ProductController(
	@Autowired val messageBus: MessageBus
) {

	@PostMapping
	fun createProduct(@RequestBody createProductRequest: CreateProductRequest): ProductCreatedResponse {
		val createProductCommand = CreateProductCommand(UUID.randomUUID().toString(), createProductRequest.name)
		val commandResponse = messageBus.send(createProductCommand).payload

		if(commandResponse.isPresent) return ProductCreatedResponse(commandResponse.get() as ProductCreatedCommandResponse)
		else throw MessageBusException()
	}

	@GetMapping("/all")
	fun getProductById(@RequestParam(value = "productId") id: String): ProductRequest {
		return ProductRequest("::id::", "::name::")
	}

}

class MessageBusException : RuntimeException()
data class CreateProductRequest(val name: String)
data class ProductCreatedResponse(val id: String, val name: String) {
	constructor(response: ProductCreatedCommandResponse) : this(response.productId, response.productName)
}
data class ProductRequest(val id: String, val name: String)