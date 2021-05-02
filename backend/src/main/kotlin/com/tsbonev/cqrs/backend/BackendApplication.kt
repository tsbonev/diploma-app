package com.tsbonev.cqrs.backend

import com.google.gson.Gson
import com.tsbonev.cqrs.backend.domain.CreateProductCommand
import com.tsbonev.cqrs.backend.domain.Product
import com.tsbonev.cqrs.backend.domain.ProductCreatedEvent
import com.tsbonev.cqrs.backend.domain.ProductWorkflow
import com.tsbonev.cqrs.backend.domain.StubIdentityProvider
import com.tsbonev.cqrs.core.AuthoredAggregateRepository
import com.tsbonev.cqrs.core.SimpleIdentityAggregateRepository
import com.tsbonev.cqrs.core.eventstore.EventStore
import com.tsbonev.cqrs.core.messagebus.Command
import com.tsbonev.cqrs.core.messagebus.CommandResponse
import com.tsbonev.cqrs.core.messagebus.Event
import com.tsbonev.cqrs.core.messagebus.Interceptor
import com.tsbonev.cqrs.core.messagebus.MessageBus
import com.tsbonev.cqrs.core.messagebus.SimpleMessageBus
import com.tsbonev.cqrs.core.messagebus.SyncEventPublisher
import com.tsbonev.cqrs.core.snapshot.MessageFormat
import com.tsbonev.cqrs.core.messagebus.Workflow
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.stereotype.Component
import java.io.InputStream
import java.io.InputStreamReader

@SpringBootApplication
class BackendApplication

fun main(args: Array<String>) {
	runApplication<BackendApplication>(*args)
}

@Component
class MessageBusConfiguration(@Autowired private val eventStore: EventStore,
                              private val messageBus: SimpleMessageBus = SimpleMessageBus()) : MessageBus {

	init {
		val gsonMessageFormat = GSONMessageFormat(Product::class.java, ProductCreatedEvent::class.java, CreateProductCommand::class.java)
		val eventPublisher = SyncEventPublisher(SimpleMessageBus(), gsonMessageFormat)
		val aggregates = AuthoredAggregateRepository(
			StubIdentityProvider(),
			SimpleIdentityAggregateRepository(
				eventStore,
				gsonMessageFormat,
				eventPublisher
			)
		)

		messageBus.registerWorkflow(ProductWorkflow(aggregates))
	}

	override fun registerWorkflow(workflow: Workflow) {
		messageBus.registerWorkflow(workflow)
	}

	override fun registerInterceptor(interceptor: Interceptor) {
		messageBus.registerInterceptor(interceptor)
	}

	override fun <T : Command> send(command: T): CommandResponse {
		return messageBus.send(command)
	}

	override fun publish(event: Event) {
		messageBus.publish(event)
	}
}

class GSONMessageFormat(vararg types: Class<*>) : MessageFormat {
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
}