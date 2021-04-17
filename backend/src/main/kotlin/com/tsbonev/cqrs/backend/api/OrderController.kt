package com.tsbonev.cqrs.backend.api

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.Instant


@RestController
@RequestMapping("/orders")
class OrderController {

	@GetMapping("/greeting")
	fun greeting(@RequestParam(value = "name", defaultValue = "World") name: String?): OrderRequest? {
		return OrderRequest("abv", listOf(), Instant.now())
	}

}


data class OrderRequest(val id: String, val items: List<String>, val timeOfOrder: Instant)