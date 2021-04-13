package com.tsbonev.cqrs.core

import com.tsbonev.cqrs.core.eventstore.GetEventsFromStreamsRequest
import com.tsbonev.cqrs.core.eventstore.GetEventsResponse
import com.tsbonev.cqrs.core.eventstore.SaveEventsResponse
import com.tsbonev.cqrs.core.helpers.InMemoryEventPublisher
import com.tsbonev.cqrs.core.helpers.InMemoryEventStore
import com.tsbonev.cqrs.core.contracts.TestMessageFormat
import org.hamcrest.CoreMatchers
import org.junit.Assert
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.UUID
import org.hamcrest.CoreMatchers.`is` as Is
import org.junit.Assert.assertThat

class SimpleIdentityAggregateRepositoryTest {

	private val configuration = object : AggregateConfiguration {
		override fun topicName(aggregate: AggregateRoot): String {
			return "::topic::"
		}
	}

	private val messageFormat = TestMessageFormat(
		InvoiceCreatedEvent::class.java,
		ChangeCustomerName::class.java,
		ChangeListEvent::class.java,
		TestClassCreatedEvent::class.java,
		ChangeStringEvent::class.java,
		ChangeLongEvent::class.java,
		ChangeObjectEvent::class.java,
		ChangeListEvent::class.java
	)

	private val anyIdentity = Identity("::user id::", "tenant1", LocalDateTime.of(2018, 4, 1, 10, 12, 34).toInstant(
		ZoneOffset.UTC))

	@Test
	fun `Saves aggregate with identity`() {
		val invoice = Invoice(invoiceId(), "John")
		val eventRepository = SimpleIdentityAggregateRepository(InMemoryEventStore(5), messageFormat, InMemoryEventPublisher(), configuration)
		eventRepository.save(invoice, anyIdentity)

		val loadedInvoice = eventRepository.getById(invoice.getId(), Invoice::class.java, anyIdentity)
		assertThat(loadedInvoice.customerName, CoreMatchers.equalTo("John"))
	}

	@Test
	fun `Retrieves event streams`() {
		val invoice1 = Invoice("invoice1", "John")
		val invoice2 = Invoice("invoice1", "John")

		val eventRepository = SimpleIdentityAggregateRepository(InMemoryEventStore(5), messageFormat, InMemoryEventPublisher(), configuration)

		eventRepository.save(invoice1, anyIdentity)
		eventRepository.save(invoice2, anyIdentity)
	}

	@Test
	fun `Applies changes and updates`() {
		val initialInvoice = Invoice(invoiceId(), "John")

		val eventPublisher = InMemoryEventPublisher()
		val eventRepository = SimpleIdentityAggregateRepository(InMemoryEventStore(5), messageFormat, eventPublisher, configuration)
		eventRepository.save(initialInvoice, anyIdentity)

		var invoice = eventRepository.getById(initialInvoice.getId(), Invoice::class.java, anyIdentity)

		invoice.changeCustomerName("Peter")
		eventRepository.save(invoice, anyIdentity)

		invoice = eventRepository.getById(invoice.getId(), Invoice::class.java, anyIdentity)

		assertThat(invoice.customerName, CoreMatchers.equalTo("Peter"))
		assertThat(eventPublisher.events.size, CoreMatchers.equalTo(2))
	}

	@Test
	fun `Publishes events after save`() {
		val invoice = Invoice(invoiceId(), "John")
		val eventPublisher = InMemoryEventPublisher()
		val eventRepository = SimpleIdentityAggregateRepository(
			InMemoryEventStore(5),
			messageFormat,
			eventPublisher,
			configuration
		)
		eventRepository.save(invoice, anyIdentity)

		assertThat(
			eventPublisher.events, CoreMatchers.equalTo(
				listOf(
					EventWithBinaryPayload(
						InvoiceCreatedEvent(invoice.getId(), "John"),
						BinaryPayload("""{"invoiceId":"${invoice.getId()}","customerName":"John"}""")
					)
				)
			)
		)
	}

	@Test(expected = EventCollisionException::class)
	fun `Causes event collision`() {
		val invoice = Invoice(invoiceId(), "John")
		val eventStore = InMemoryEventStore(5)
		val eventRepository = SimpleIdentityAggregateRepository(eventStore, messageFormat, InMemoryEventPublisher(), configuration)

		eventStore.pretendThatNextSaveWillReturn(SaveEventsResponse.EventCollision(3L))

		eventRepository.save(invoice, anyIdentity)
	}

	@Test
	fun `Rolls back events`() {
		val invoiceId = invoiceId()
		val invoice = Invoice(invoiceId, "John")
		val eventPublisher = InMemoryEventPublisher()
		val eventStore = InMemoryEventStore(5)
		val eventRepository = SimpleIdentityAggregateRepository(eventStore, messageFormat, eventPublisher, configuration)

		eventPublisher.pretendThatNextPublishWillFail()

		try {
			eventRepository.save("Invoice_$invoiceId", invoice, anyIdentity)
			Assert.fail("exception wasn't re-thrown when publishing failed?")
		} catch (ex: PublishErrorException) {
			val response = eventStore.getEventsFromStreams(GetEventsFromStreamsRequest(anyIdentity.tenant, "Invoice_$invoiceId")) as GetEventsResponse.Success
			assertThat(response.aggregates.isEmpty(), Is(true))
		}
	}

	@Test
	fun `Rolls back only failed events`() {
		val invoice = Invoice(invoiceId(), "John")
		val eventStore = InMemoryEventStore(5)
		val eventPublisher = InMemoryEventPublisher()
		val eventRepository = SimpleIdentityAggregateRepository(
			eventStore,
			messageFormat,
			eventPublisher,
			configuration
		)

		eventRepository.save(invoice, anyIdentity)

		invoice.changeCustomerName("Peter")

		eventPublisher.pretendThatNextPublishWillFail()
		try {
			eventRepository.save(invoice, anyIdentity)
			Assert.fail("exception wasn't re-thrown when publishing failed?")
		} catch (ex: PublishErrorException) {
			val response = eventStore.getEventsFromStreams(GetEventsFromStreamsRequest(anyIdentity.tenant, "Invoice_${invoice.getId()}")) as GetEventsResponse.Success
			assertThat(response.aggregates[0].events.size, Is(1))
		}
	}

	@Test(expected = AggregateNotFoundException::class)
	fun `Retrieve uknown aggregate`() {
		val eventRepository = SimpleIdentityAggregateRepository(
			InMemoryEventStore(5),
			messageFormat,
			InMemoryEventPublisher(),
			configuration
		)

		eventRepository.getById("::any id::", Invoice::class.java, anyIdentity)
	}

	@Test
	fun `Retrieve mutliple aggregates`() {
		val firstInvoice = Invoice(invoiceId(), "John")
		val secondInvoice = Invoice(invoiceId(), "Peter")
		val eventRepository = SimpleIdentityAggregateRepository(InMemoryEventStore(5), messageFormat, InMemoryEventPublisher(), configuration)
		eventRepository.save(firstInvoice, anyIdentity)
		eventRepository.save(secondInvoice, anyIdentity)

		val loadedInvoices = eventRepository.getByIds(listOf(firstInvoice.getId(), secondInvoice.getId()), Invoice::class.java, anyIdentity)
		assertThat(
			loadedInvoices, Is(
				CoreMatchers.equalTo(
					mapOf(
						firstInvoice.getId() to firstInvoice,
						secondInvoice.getId() to secondInvoice
					)
				)
			)
		)
	}

	@Test
	fun `Get one out of two invoices`() {
		val firstInvoice = Invoice(invoiceId(), "John")
		val eventRepository = SimpleIdentityAggregateRepository(InMemoryEventStore(5), messageFormat, InMemoryEventPublisher(), configuration)
		eventRepository.save(firstInvoice, anyIdentity)

		val loadedInvoices = eventRepository.getByIds(listOf(firstInvoice.getId(), "::any unknown id::"), Invoice::class.java, anyIdentity)
		assertThat(
			loadedInvoices, Is(
				CoreMatchers.equalTo(
					mapOf(
						firstInvoice.getId() to firstInvoice
					)
				)
			)
		)
	}

	@Test
	fun `Returns no aggregates`() {
		val eventRepository = SimpleIdentityAggregateRepository(InMemoryEventStore(5), messageFormat, InMemoryEventPublisher(), configuration)

		val invoices = eventRepository.getByIds(listOf("::id 1::", "::id 2::"), Invoice::class.java, anyIdentity)
		assertThat(invoices, Is(CoreMatchers.equalTo(mapOf())))
	}

	@Test
	fun `Creates snapshot at limit`() {
		var aggregate = TestAggregate("::id::", "::string::", 1, TestObject("::value::"), listOf(TestObject("::value2::")))

		val eventPublisher = InMemoryEventPublisher()
		val eventStore = InMemoryEventStore(1)
		val eventRepository = SimpleIdentityAggregateRepository(eventStore, messageFormat, eventPublisher, configuration)
		eventRepository.save(aggregate, anyIdentity)


		assertThat(eventStore.saveEventOptions.last.version, Is(0L))

		aggregate = eventRepository.getById(aggregate.getId(), TestAggregate::class.java, anyIdentity)

		aggregate.changeLong(123)
		eventRepository.save(aggregate, anyIdentity)

		assertThat(eventStore.saveEventOptions.last.version, Is(1L))
		assertThat(eventStore.saveEventOptions.last.createSnapshotRequest.snapshot!!.version, Is(1L))

		aggregate = eventRepository.getById(aggregate.getId(), TestAggregate::class.java, anyIdentity)

		assertThat(aggregate.getExpectedVersion(), Is(2L)) // after the last event
		assertThat(aggregate.long, CoreMatchers.equalTo(123L))
	}

	@Test
	fun `Creates multiple snapshots`() {
		val eventPublisher = InMemoryEventPublisher()
		val eventRepository = SimpleIdentityAggregateRepository(InMemoryEventStore(1), messageFormat, eventPublisher, configuration)

		var aggregate = TestAggregate("::id::", "::string::", 1, TestObject("::value::", Foo("bar")), listOf(TestObject("::value2::", Foo("baar"))))

		eventRepository.save(aggregate, anyIdentity)
		aggregate = eventRepository.getById(aggregate.getId(), TestAggregate::class.java, anyIdentity)

		aggregate.changeLong(123)
		eventRepository.save(aggregate, anyIdentity)
		aggregate = eventRepository.getById(aggregate.getId(), TestAggregate::class.java, anyIdentity)

		aggregate.changeString("newString")
		eventRepository.save(aggregate, anyIdentity)
		aggregate = eventRepository.getById(aggregate.getId(), TestAggregate::class.java, anyIdentity)

		aggregate.changeObject(TestObject("otherValue", Foo("FooBar")))
		eventRepository.save(aggregate, anyIdentity)
		aggregate = eventRepository.getById(aggregate.getId(), TestAggregate::class.java, anyIdentity)

		aggregate.changeList(listOf(TestObject("otherValueInList", Foo("BarFoo"))))
		eventRepository.save(aggregate, anyIdentity)

		aggregate = eventRepository.getById(aggregate.getId(), TestAggregate::class.java, anyIdentity)

		assertThat(aggregate.long, CoreMatchers.equalTo(123L))
		assertThat(aggregate.string, CoreMatchers.equalTo("newString"))
		assertThat(aggregate.testObject, CoreMatchers.equalTo(TestObject("otherValue", Foo("FooBar"))))
		assertThat(aggregate.list[0], CoreMatchers.equalTo(TestObject("otherValueInList", Foo("BarFoo"))))
	}

	@Test
	fun `Uses default snapshot mapper`() {
		val eventRepository = SimpleIdentityAggregateRepository(InMemoryEventStore(1), messageFormat, InMemoryEventPublisher(), configuration)
		val id = invoiceId()

		var invoice = Invoice(id, "John")
		eventRepository.save(invoice, anyIdentity)

		invoice.changeCustomerName("Smith")
		eventRepository.save(invoice, anyIdentity)
		invoice = eventRepository.getById(id, Invoice::class.java, anyIdentity)

		invoice.changeCustomerName("Foo")
		eventRepository.save(invoice, anyIdentity)
		invoice = eventRepository.getById(id, Invoice::class.java, anyIdentity)

		assertThat(invoice.customerName, Is(CoreMatchers.equalTo("Foo")))
	}

	private fun invoiceId() = UUID.randomUUID().toString()

	data class InvoiceCreatedEvent(@JvmField val invoiceId: String, @JvmField val customerName: String) : Event

	data class ChangeCustomerName(@JvmField val invoiceId: String, @JvmField val newCustomerName: String) : Event

	data class Invoice private constructor(@JvmField var customerName: String) : AggregateRootBase() {

		constructor() : this("")

		constructor(id: String, customerName: String) : this(customerName) {
			applyChange(InvoiceCreatedEvent(id, customerName))
		}

		fun changeCustomerName(customerName: String) {
			applyChange(ChangeCustomerName(getId(), customerName))
		}

		fun apply(event: InvoiceCreatedEvent) {
			aggregateId = event.invoiceId
			customerName = event.customerName
		}

		fun apply(event: ChangeCustomerName) {
			customerName = event.newCustomerName
		}
	}

}

data class TestAggregate private constructor(var string: String, var long: Long, var testObject: TestObject, var list: List<TestObject>) : AggregateRootBase() {
	constructor() : this("", 0, TestObject(), listOf())

	constructor(id: String, string: String, long: Long, testObject: TestObject, list: List<TestObject>) : this(string, long, testObject, list) {
		applyChange(TestClassCreatedEvent(id, string, long, testObject, list))
	}

	fun changeString(newString: String) {
		applyChange(ChangeStringEvent(newString))
	}

	fun changeLong(newLong: Long) {
		applyChange(ChangeLongEvent(newLong))
	}

	fun changeObject(newObject: TestObject) {
		applyChange(ChangeObjectEvent(newObject))
	}

	fun changeList(newList: List<TestObject>) {
		applyChange(ChangeListEvent(newList))
	}

	fun apply(event: TestClassCreatedEvent) {
		aggregateId = event.id
		string = event.string
		long = event.long
		testObject = event.testObject
		list = event.list
	}

	fun apply(event: ChangeLongEvent) {
		long = event.newLong
	}

	fun apply(event: ChangeStringEvent) {
		string = event.newString
	}

	fun apply(event: ChangeObjectEvent) {
		testObject = event.newObject
	}

	fun apply(event: ChangeListEvent) {
		list = event.newList
	}
}

data class TestClassCreatedEvent(val id: String, val string: String, val long: Long, val testObject: TestObject, val list: List<TestObject>) : Event

data class ChangeStringEvent(val newString: String) : Event

data class ChangeLongEvent(val newLong: Long) : Event

data class ChangeObjectEvent(val newObject: TestObject) : Event

data class ChangeListEvent(val newList: List<TestObject>) : Event

data class TestObject(val value: String = "", val innerClass: Foo = Foo())

data class Foo(val bar: String = "")