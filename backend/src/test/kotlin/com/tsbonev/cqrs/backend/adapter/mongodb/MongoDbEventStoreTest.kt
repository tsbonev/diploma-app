package com.tsbonev.cqrs.backend.adapter.mongodb

import com.google.gson.Gson
import com.mongodb.MongoClient
import com.mongodb.ServerAddress
import com.tsbonev.cqrs.core.DataModelFormat
import com.tsbonev.cqrs.core.contracts.EventStoreContract
import com.tsbonev.cqrs.core.eventstore.EventStore
import com.tsbonev.cqrs.core.snapshot.MessageFormat
import de.bwaldvogel.mongo.MongoServer
import de.bwaldvogel.mongo.backend.memory.MemoryBackend
import org.junit.jupiter.api.AfterAll
import java.io.InputStream
import java.io.InputStreamReader
import java.lang.reflect.Type

/**
 * @author Tsvetozar Bonev (tsbonev@gmail.com)
 */
class MongoDbEventStoreTest : EventStoreContract() {

	companion object {
		private lateinit var client: MongoClient
		private lateinit var server: MongoServer
		private lateinit var aggregateBase: MongoDbEventStore

		@AfterAll
		fun tearDown() {
			client.close()
			server.shutdown()
		}
	}


	override fun createEventStore(): EventStore {
		server = MongoServer(MemoryBackend())
		val serverAddress = server.bind()
		client = MongoClient(ServerAddress(serverAddress))

		aggregateBase = MongoDbEventStore(
			"db",
			"Events",
			TestMessageFormat(),
			client,
			true,
			1000000
		)

		return aggregateBase
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