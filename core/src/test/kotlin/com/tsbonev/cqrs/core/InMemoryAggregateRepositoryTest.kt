package com.tsbonev.cqrs.core

import com.tsbonev.cqrs.core.helpers.InMemoryAggregateRepository
import com.tsbonev.cqrs.core.messagebus.Event
import org.hamcrest.CoreMatchers
import org.junit.jupiter.api.Test
import org.hamcrest.CoreMatchers.`is` as Is
import org.hamcrest.MatcherAssert.assertThat


class InMemoryAggregateRepositoryTest {

	@Test
	fun `Saves aggregate`() {
		val aggregateRepository = InMemoryAggregateRepository()
		val order = Order("1", "My Customer")

		aggregateRepository.save(order)

		val loaded = aggregateRepository.getById(order.getId(), Order::class.java)
		assertThat(loaded.customerName, Is(CoreMatchers.equalTo("My Customer")))
	}

	@Test
	fun `Updates aggregate root`() {
		val aggregateRepository = InMemoryAggregateRepository()
		val order = Order("1", "My Customer")

		aggregateRepository.save(order)

		val loadedBeforeChange = aggregateRepository.getById(order.getId(), Order::class.java)
		loadedBeforeChange.updateCustomer("New Name")
		aggregateRepository.save(loadedBeforeChange)

		val loadedAfterChange = aggregateRepository.getById(order.getId(), Order::class.java)

		assertThat(loadedAfterChange.getId(), Is(CoreMatchers.equalTo("1")))
		assertThat(loadedAfterChange.customerName, Is(CoreMatchers.equalTo("New Name")))
	}

	@Test
	fun `Gets many aggregates`() {
		val aggregateRepository = InMemoryAggregateRepository()

		aggregateRepository.save(Order("1", "Customer A"))
		aggregateRepository.save(Order("2", "Customer B"))

		val loaded = aggregateRepository.getByIds(listOf("1", "2"), Order::class.java)
		assertThat(loaded.size, Is(2))
		assertThat(loaded["1"]!!.customerName, Is(CoreMatchers.equalTo("Customer A")))
		assertThat(loaded["2"]!!.customerName, Is(CoreMatchers.equalTo("Customer B")))
	}

	@Test
	fun `No aggregates found`() {
		val aggregateRepository = InMemoryAggregateRepository()

		val loaded = aggregateRepository.getByIds(listOf("1", "2"), Order::class.java)
		assertThat(loaded.size, Is(0))
	}

	@Test
	fun `Retrieves only existing aggregate`() {
		val aggregateRepository = InMemoryAggregateRepository()

		aggregateRepository.save(Order("2", "Customer B"))

		val loaded = aggregateRepository.getByIds(listOf("1", "2"), Order::class.java)
		assertThat(loaded.size, Is(1))
		assertThat(loaded["2"]!!.customerName, Is(CoreMatchers.equalTo("Customer B")))
	}
}

internal class Order private constructor(var customerName: String) : AggregateRootBase() {

	@Suppress("UNUSED")
	constructor() : this("")

	constructor(id: String, customerName: String) : this(customerName) {
		applyChange(OrderCreatedEvent(id, customerName))
	}

	fun updateCustomer(newName: String) {
		applyChange(CustomerUpdatedEvent(newName))
	}

	fun apply(event: OrderCreatedEvent) {
		aggregateId = event.id
		customerName = event.customerName
	}

	fun apply(event: CustomerUpdatedEvent) {
		customerName = event.newCustomerName
	}
}

data class OrderCreatedEvent(val id: String, val customerName: String) : Event

data class CustomerUpdatedEvent(val newCustomerName: String) : Event