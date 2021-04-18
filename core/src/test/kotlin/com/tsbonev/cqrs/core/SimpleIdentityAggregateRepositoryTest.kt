package com.tsbonev.cqrs.core

import com.google.gson.Gson
import com.tsbonev.cqrs.core.eventstore.SaveEventsResponse
import com.tsbonev.cqrs.core.helpers.InMemoryEventPublisher
import com.tsbonev.cqrs.core.helpers.InMemoryEventStore
import com.tsbonev.cqrs.core.messagebus.Event
import com.tsbonev.cqrs.core.snapshot.MessageFormat
import org.hamcrest.CoreMatchers
import org.hamcrest.MatcherAssert.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.fail
import java.io.InputStream
import java.io.InputStreamReader
import java.lang.reflect.Type
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.UUID
import org.hamcrest.CoreMatchers.`is` as Is

class SimpleIdentityAggregateRepositoryTest {

	private val messageFormat = TestMessageFormat(
		InvoiceCreatedEvent::class.java,
		ChangeCustomerName::class.java,
		ChangeListEvent::class.java,
		TestClassCreatedEvent::class.java,
		ChangeStringEvent::class.java,
		ChangeLongEvent::class.java,
		ChangeObjectEvent::class.java,
		ChangeListEvent::class.java,
		TestAggregate::class.java,
		Invoice::class.java
	)

	private val anyIdentity = Identity(
		"::user id::", LocalDateTime.of(2018, 4, 1, 10, 12, 34).toInstant(
			ZoneOffset.UTC
		)
	)

	@Test
	fun `Saves aggregate with identity`() {
		val invoice = Invoice(invoiceId(), "John")
		val eventRepository = SimpleIdentityAggregateRepository(
			InMemoryEventStore(5),
			messageFormat,
			InMemoryEventPublisher(messageFormat)
		)
		eventRepository.save(invoice, anyIdentity)

		val loadedInvoice = eventRepository.getById(invoice.getId(), Invoice::class.java, anyIdentity)
		assertThat(loadedInvoice.customerName, CoreMatchers.equalTo("John"))
	}

	@Test
	fun `Retrieves event streams`() {
		val invoice1 = Invoice("invoice1", "John")
		val invoice2 = Invoice("invoice1", "John")

		val eventRepository = SimpleIdentityAggregateRepository(
			InMemoryEventStore(5),
			messageFormat,
			InMemoryEventPublisher(messageFormat)
		)

		eventRepository.save(invoice1, anyIdentity)
		eventRepository.save(invoice2, anyIdentity)
	}

	@Test
	fun `Applies changes and updates`() {
		val initialInvoice = Invoice(invoiceId(), "John")

		val eventPublisher = InMemoryEventPublisher(messageFormat)
		val eventRepository = SimpleIdentityAggregateRepository(InMemoryEventStore(5), messageFormat, eventPublisher)
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
		val eventPublisher = InMemoryEventPublisher(messageFormat)
		val eventRepository = SimpleIdentityAggregateRepository(
			InMemoryEventStore(5),
			messageFormat,
			eventPublisher
		)
		eventRepository.save(invoice, anyIdentity)

		assertThat(
			eventPublisher.events, CoreMatchers.equalTo(
				listOf(InvoiceCreatedEvent(invoice.getId(), "John")),
			)
		)
	}

	@Test
	fun `Causes event collision`() {
		val invoice = Invoice(invoiceId(), "John")
		val eventStore = InMemoryEventStore(5)
		val eventRepository = SimpleIdentityAggregateRepository(eventStore, messageFormat, InMemoryEventPublisher(messageFormat))

		eventStore.pretendThatNextSaveWillReturn(SaveEventsResponse.EventCollision(3L, 0L))

		assertThrows<EventCollisionException> {
			eventRepository.save(invoice, anyIdentity)
		}
	}

	@Test
	fun `Rolls back events`() {
		val invoiceId = invoiceId()
		val invoice = Invoice(invoiceId, "John")
		val eventPublisher = InMemoryEventPublisher(messageFormat)
		val eventStore = InMemoryEventStore(5)
		val eventRepository = SimpleIdentityAggregateRepository(eventStore, messageFormat, eventPublisher)

		eventPublisher.pretendThatNextPublishWillFail()

		try {
			eventRepository.save(invoice, anyIdentity)
			fail("exception wasn't re-thrown when publishing failed?")
		} catch (ex: PublishErrorException) {
			val response = eventStore.getEvents(invoice.getId())
			assertThat(response.events.events.isEmpty(), Is(true))
		}
	}

	@Test
	fun `Rolls back only failed events`() {
		val invoice = Invoice(invoiceId(), "John")
		val eventStore = InMemoryEventStore(5)
		val eventPublisher = InMemoryEventPublisher(messageFormat)
		val eventRepository = SimpleIdentityAggregateRepository(
			eventStore,
			messageFormat,
			eventPublisher
		)

		eventRepository.save(invoice, anyIdentity)

		invoice.changeCustomerName("Peter")

		eventPublisher.pretendThatNextPublishWillFail()
		try {
			eventRepository.save(invoice, anyIdentity)
			fail("exception wasn't re-thrown when publishing failed?")
		} catch (ex: PublishErrorException) {
			val response = eventStore.getEvents(invoice.getId())
			assertThat(response.events.events.size, Is(1))
		}
	}

	@Test
	fun `Retrieve unknown aggregate`() {
		val eventRepository = SimpleIdentityAggregateRepository(
			InMemoryEventStore(5),
			messageFormat,
			InMemoryEventPublisher(messageFormat)
		)

		assertThrows<AggregateNotFoundException> {
			eventRepository.getById("::any id::", Invoice::class.java, anyIdentity)
		}
	}

	@Test
	fun `Retrieve mutliple aggregates`() {
		val firstInvoice = Invoice(invoiceId(), "John")
		val secondInvoice = Invoice(invoiceId(), "Peter")
		val eventRepository = SimpleIdentityAggregateRepository(
			InMemoryEventStore(5),
			messageFormat,
			InMemoryEventPublisher(messageFormat)
		)
		eventRepository.save(firstInvoice, anyIdentity)
		eventRepository.save(secondInvoice, anyIdentity)

		val loadedInvoices = eventRepository.getByIds(
			listOf(firstInvoice.getId(), secondInvoice.getId()),
			Invoice::class.java,
			anyIdentity
		)
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
		val eventRepository = SimpleIdentityAggregateRepository(
			InMemoryEventStore(5),
			messageFormat,
			InMemoryEventPublisher(messageFormat)
		)

		eventRepository.save(firstInvoice, anyIdentity)

		val loadedInvoices = eventRepository.getByIds(
			listOf(firstInvoice.getId(), "::any unknown id::"),
			Invoice::class.java,
			anyIdentity
		)

		assertThat(loadedInvoices, Is(CoreMatchers.equalTo(mapOf(firstInvoice.getId() to Invoice(firstInvoice.getId(), "John")))))
	}

	@Test
	fun `Returns no aggregates`() {
		val eventRepository = SimpleIdentityAggregateRepository(
			InMemoryEventStore(5),
			messageFormat,
			InMemoryEventPublisher(messageFormat)
		)

		val invoices = eventRepository.getByIds(
			listOf("::id 1::", "::id 2::"),
			Invoice::class.java, anyIdentity
		)
		assertThat(invoices, Is(CoreMatchers.equalTo(mapOf())))
	}

	@Test
	fun `Creates snapshot at limit`() {
		var aggregate = TestAggregate(
			"::id::", "::string::", 1, TestObject("::value::"),
			listOf(TestObject("::value2::"))
		)

		val eventPublisher = InMemoryEventPublisher(messageFormat)
		val eventStore = InMemoryEventStore(1)
		val eventRepository = SimpleIdentityAggregateRepository(eventStore, messageFormat, eventPublisher)
		eventRepository.save(aggregate, anyIdentity)

		aggregate = eventRepository.getById(aggregate.getId(), TestAggregate::class.java, anyIdentity)

		aggregate.changeLong(123)
		eventRepository.save(aggregate, anyIdentity)

		aggregate = eventRepository.getById(aggregate.getId(), TestAggregate::class.java, anyIdentity)

		assertThat(aggregate.getExpectedVersion(), Is(2L))
		assertThat(aggregate.long, CoreMatchers.equalTo(123L))
	}

	@Test
	fun `Creates multiple snapshots`() {
		val eventPublisher = InMemoryEventPublisher(messageFormat)
		val eventRepository = SimpleIdentityAggregateRepository(
			InMemoryEventStore(1),
			messageFormat, eventPublisher
		)

		var aggregate = TestAggregate(
			"::id::", "::string::", 1,
			TestObject("::value::", Foo("bar")),
			listOf(TestObject("::value2::", Foo("baar")))
		)

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
		val eventRepository = SimpleIdentityAggregateRepository(
			InMemoryEventStore(1), messageFormat,
			InMemoryEventPublisher(messageFormat)
		)
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

data class TestAggregate private constructor(
	var string: String, var long: Long, var testObject: TestObject,
	var list: List<TestObject>
) : AggregateRootBase() {
	constructor() : this("", 0, TestObject(), listOf())

	constructor(id: String, string: String, long: Long, testObject: TestObject, list: List<TestObject>) : this(
		string,
		long,
		testObject,
		list
	) {
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

class TestMessageFormat(vararg types: Class<*>) : MessageFormat, DataModelFormat {
	private val gson = Gson()
	private val kindToType = mutableMapOf<String, Class<*>>()

	init {
		types.forEach {
			kindToType[it.simpleName] = it
		}
	}

	override fun supportsKind(kind: String): Boolean {
		return kindToType.containsKey(kind)
	}

	@Suppress("UNCHECKED_CAST")
	override fun <T> parse(stream: InputStream, kind: String): T {
		val type = kindToType.getValue(kind)
		return gson.fromJson(InputStreamReader(stream, Charsets.UTF_8), type) as T
	}

	override fun formatToBytes(value: Any): ByteArray {
		return gson.toJson(value).toByteArray(Charsets.UTF_8)
	}

	override fun <T> parse(stream: InputStream, type: Type): T {
		return gson.fromJson(InputStreamReader(stream, Charsets.UTF_8), type)
	}

	override fun formatToString(value: Any): String {
		return gson.toJson(value)
	}
}

data class TestClassCreatedEvent(
	val id: String, val string: String, val long: Long, val testObject: TestObject,
	val list: List<TestObject>
) :
	Event

data class ChangeStringEvent(val newString: String) : Event

data class ChangeLongEvent(val newLong: Long) : Event

data class ChangeObjectEvent(val newObject: TestObject) : Event

data class ChangeListEvent(val newList: List<TestObject>) : Event

data class TestObject(val value: String = "", val innerClass: Foo = Foo())

data class Foo(val bar: String = "")