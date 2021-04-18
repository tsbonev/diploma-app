package com.tsbonev.cqrs.backend.adapter.mongodb

import com.tsbonev.cqrs.core.*
import com.mongodb.MongoClient
import com.mongodb.client.MongoCollection
import com.mongodb.client.model.Filters.*
import com.tsbonev.cqrs.core.eventstore.Aggregate
import com.tsbonev.cqrs.core.eventstore.EventPayload
import com.tsbonev.cqrs.core.eventstore.EventStore
import com.tsbonev.cqrs.core.eventstore.GetAllEventsRequest
import com.tsbonev.cqrs.core.eventstore.GetAllEventsResponse
import com.tsbonev.cqrs.core.eventstore.GetEventsFromStreamsRequest
import com.tsbonev.cqrs.core.eventstore.GetEventsResponse
import com.tsbonev.cqrs.core.eventstore.RevertEventsResponse
import com.tsbonev.cqrs.core.eventstore.SaveEventsRequest
import com.tsbonev.cqrs.core.eventstore.SaveEventsResponse
import com.tsbonev.cqrs.core.eventstore.SaveOptions
import com.tsbonev.cqrs.core.snapshot.Snapshot
import org.bson.Document
import java.io.ByteArrayInputStream

/**
 * @author Tsvetozar Bonev (tsbonev@gmail.com)
 */
class MongoDbEventStore(private val database: String = "db",
                        private val eventsName: String = "Events",
                        private val messageFormat: DataModelFormat,
                        private val mongoClient: MongoClient,
                        private val inTestEnvironment: Boolean = false,
                        private val documentSizeLimit: Long = 16793600L,
						private val idGenerator: IdGenerator = IdGenerators.snowflake()) : EventStore {

	/**
	 * Collection name used for storing of snapshots.
	 */
	private val snapshotsName = eventsName + "Snapshots"

	/**
	 * Collection name used for storing event indexed
	 */
	private val indexName = "EventIndexes"

	/**
	 * Property name for the aggregate type.
	 */
	private val aggregateTypeProperty = "a"

	/**
	 * Property name for the list of event data in the document.
	 */
	private val eventsProperty = "e"

	/**
	 * Property name of the version which is used for concurrency control.
	 */
	private val versionProperty = "v"

	/**
	 * Collection to store snapshots.
	 */
	private val snapshotsCollection: MongoCollection<Document>
		get() = mongoClient.getDatabase(database).getCollection(snapshotsName)

	/**
	 * Collection to store aggregates.
	 */
	private val eventsCollection: MongoCollection<Document>
		get() = mongoClient.getDatabase(database).getCollection(eventsName)

	/**
	 * Collection to store event indexes
	 */
	private val indexesCollection: MongoCollection<Document>
		get() = mongoClient.getDatabase(database).getCollection(indexName)

	override fun saveEvents(request: SaveEventsRequest, saveOptions: SaveOptions): SaveEventsResponse {
		val version = saveOptions.version
		val aggregateType = request.aggregateType
		val stream = request.stream
		var snapshot: Snapshot? = null

		val aggregateId = request.stream

		/**
		 * If in a test environment, the transactions are
		 * stubbed as bwaldvogel:mongo-java-server:1.8.0
		 * throws an exception if one is started.
		 */
		val session = if (!inTestEnvironment) {
			mongoClient.startSession()
		} else MongoClientSessionStub()

		try {
			session.startTransaction()

			val snapshotDocument = snapshotsCollection.find(eq("_id", aggregateId)).first()
				?: Document(mapOf("_id" to aggregateId))
			var aggregateIndex = snapshotDocument.get("aggregateIndex", 0L) as Long

			val aggregateKey = aggregateKey(aggregateId, aggregateIndex)

			val aggregateDocument =
				eventsCollection.find(eq("_id", aggregateKey)).first()
					?: Document(
						mapOf(
							"_id" to aggregateKey,
							eventsProperty to mutableListOf<String>(),
							versionProperty to 0L,
							aggregateTypeProperty to request.aggregateType
						)
					)

			val documentVersion = aggregateDocument.getLong(versionProperty)

			if (documentVersion != saveOptions.version) {
				throw EventCollisionException(stream, documentVersion)
			}

			val events = request.events.mapIndexed { index, it ->
				EventModel(
					it.aggregateId, it.aggregateType, documentVersion + index + 1, it.identityId, it.timestamp,
					it.data.payload.toString(Charsets.UTF_8)
				)
			}

			val sequenceIds = (1..events.size).map { idGenerator.nextId() }

			val eventIndexes = events.mapIndexed { index, eventModel ->
				val sequenceId = sequenceIds[index]
				mapOf(
					"s" to sequenceId,
					"t" to request.tenant,
					"ai" to eventModel.aggregateId,
					"at" to aggregateType,
					"st" to stream,
					"v" to eventModel.version,
					"r" to aggregateDocument
				)
			}

			val existingModel = if(aggregateDocument.getString("payload") == null) {
				EventsModel(listOf())
			} else {
				messageFormat.parse<EventsModel>(
					ByteArrayInputStream(aggregateDocument.getString("payload")?.toByteArray(Charsets.UTF_8)),
					EventsModel::class.java
				)
			}

			val currentVersion = documentVersion

			val eventsModel = if (saveOptions.createSnapshotRequest.required && saveOptions.createSnapshotRequest.snapshot != null) {
				val snapshotData = saveOptions.createSnapshotRequest.snapshot!!.data
				val aggregateVersion = saveOptions.createSnapshotRequest.snapshot!!.version

				snapshotDocument["version"] = aggregateVersion
				snapshotDocument["aggregateIndex"] = aggregateIndex + 1
				snapshotDocument["data"] = snapshotData.payload.toString(Charsets.UTF_8)

				snapshotsCollection.replaceOne(eq("_id", aggregateId), snapshotDocument)

				snapshot = Snapshot(aggregateIndex + 1, snapshotData)

				EventsModel(events)
			} else {
				EventsModel(existingModel.events + events)
			}

			val payloadAsText = messageFormat.formatToString(eventsModel)
			val sizeInBytes = payloadAsText.toByteArray(Charsets.UTF_8).size

			if (sizeInBytes >= documentSizeLimit) {

				var snapshot: Snapshot? = null
				//if a build snapshot does not exist it would not have the field data filled in.

				if (snapshotDocument["data"] != null) {
					val blobData = snapshotDocument["data"] as String

					snapshot = Snapshot(
						snapshotDocument["version"] as Long,
						BinaryPayload(blobData.toByteArray(Charsets.UTF_8))
					)
				}

				val newVersion = currentVersion + events.size
				aggregateDocument["version"] = newVersion
				aggregateDocument["payload"] = payloadAsText
				aggregateDocument["stream"] = stream
				aggregateDocument["aggregateType"] = aggregateType

				eventIndexes.forEach {
					val indexedVersion = it["v"]
					val indexDocument = indexesCollection.replaceOne(eq("_id", "stream_indexes/${request.tenant}_${stream}_${aggregateIndex}_$indexedVersion"),
					Document(mapOf("_id" to it)))
				}

				return SaveEventsResponse.SnapshotRequired(adaptEvents(existingModel.events), snapshot, version)
			}

		} catch (ex: Exception) {
			return SaveEventsResponse.Error("could not save events due: ${ex.message}")
		} finally {
			if (session.hasActiveTransaction()) session.abortTransaction()
			session.close()
		}

		return SaveEventsResponse.Success(1L, listOf(), Aggregate("a", snapshot, 1L, listOf()))
	}

	override fun getEventsFromStreams(request: GetEventsFromStreamsRequest): GetEventsResponse {
		val snapshotDocuments = mutableMapOf<String, Document>()
		request.streams.forEach {
			val snapshot = snapshotsCollection.find(eq("_id", it)).first()
			if (snapshot != null)
				snapshotDocuments[it] = snapshot
		}

		val keyToAggregateId = mutableMapOf<String, String>()

		val aggregateKeys = snapshotDocuments.values.map {
			val key = aggregateKey(it.getString("_id"), it.getLong("aggregateIndex") ?: 0L)
			keyToAggregateId[key] = it.getString("_id")
			key
		}

		val aggregateDocuments = mutableMapOf<String, Document>()
		aggregateKeys.forEach {
			aggregateDocuments[it] = eventsCollection.find(eq("_id", it)).first()!!
		}

		val aggregates = mutableListOf<Aggregate>()

		aggregateDocuments.keys.forEach {
			val aggregateDocument = aggregateDocuments[it]
			var snapshot: Snapshot? = null

			if (snapshotDocuments.containsKey(it)) {
				val thisSnapshot = snapshotDocuments[it]!!
				val version = thisSnapshot.getLong("aggregateIndex") ?: 0L
				if (thisSnapshot["data"] != null) {
					val data = (thisSnapshot["data"] as org.bson.types.Binary).data
					snapshot = Snapshot(
						version,
						BinaryPayload(data)
					)
				}
			}

			val aggregateEvents = aggregateDocument!![eventsProperty] as List<*>
			val currentVersion = aggregateDocument.getLong(versionProperty)
			val aggregateType = aggregateDocument.getString(aggregateTypeProperty)
			val events = adaptEventsFromByteArrays(
				aggregateEvents.filterIsInstance(String::class.java).map { it.toByteArray() })

			aggregates.add(Aggregate(aggregateType, snapshot, currentVersion, events))
		}

		return GetEventsResponse.Success(aggregates)
	}

	override fun getAllEvents(request: GetAllEventsRequest): GetAllEventsResponse {
		TODO("Not yet implemented")
	}

	override fun revertEvents(tenant: String, stream: String, count: Int): RevertEventsResponse {
		if (count == 0) {
			throw IllegalArgumentException("trying to revert zero events")
		}

		val session = if (!inTestEnvironment) {
			mongoClient.startSession()
		} else MongoClientSessionStub()

		try {
			session.startTransaction()

			val snapshotDocument = snapshotsCollection.find(eq("_id", tenant)).first()
				?: return RevertEventsResponse.Error("No tenant found.")
			val aggregateIndex = snapshotDocument.getLong("aggregateIndex") ?: 0L

			val aggregateKey = aggregateKey(tenant, aggregateIndex)

			val aggregateDocument = eventsCollection.find(eq("_id", aggregateKey)).first()
				?: return RevertEventsResponse.Error("Aggregate not found.")

			@Suppress("UNCHECKED_CAST")
			val aggregateEvents = aggregateDocument[eventsProperty] as MutableList<String>
			val currentVersion = aggregateDocument.getLong(versionProperty)

			if (count > aggregateEvents.size) {
				return RevertEventsResponse.InsufficientEventsError(aggregateEvents.size, count)
			}

			val lastEventIndex = aggregateEvents.size - count
			val updatedEvents = aggregateEvents.filterIndexed { index, _ -> index < lastEventIndex }

			aggregateDocument[eventsProperty] = updatedEvents
			aggregateDocument[versionProperty] = currentVersion - count

			eventsCollection.replaceOne(eq("_id", aggregateKey), aggregateDocument)
			snapshotsCollection.replaceOne(eq("_id", tenant), snapshotDocument)

			session.commitTransaction()
		} catch (ex: Exception) {
			return RevertEventsResponse.Error("could not save events due: ${ex.message}")
		} finally {
			if (session.hasActiveTransaction()) session.abortTransaction()
			session.close()
		}

		return RevertEventsResponse.Success(listOf())
	}

	private fun adaptEventsFromByteArrays(aggregateEvents: List<ByteArray>): List<EventPayload> {
		return aggregateEvents.map {
			messageFormat.parse<EventModel>(
				ByteArrayInputStream(it),
				EventsModel::class.java
			)
		}
			.map {
				EventPayload(
					it.aggregateId,
					it.kind,
					it.timestamp,
					it.identityId,
					BinaryPayload(it.payload.toByteArray(Charsets.UTF_8))
				)
			}
	}

	private fun adaptEvents(aggregateEvents: List<EventModel>): List<EventPayload> {
		return aggregateEvents.map {
			EventPayload(
				it.aggregateId,
				it.kind,
				it.timestamp,
				it.identityId,
				BinaryPayload(it.payload.toByteArray(Charsets.UTF_8))
			)
		}
	}

	private fun aggregateKey(aggregateId: String, aggregateIndex: Long): String {
		return "${aggregateId}_$aggregateIndex"
	}
}