package com.tsbonev.cqrs.backend.adapter.mongodb

import com.mongodb.ClientSessionOptions
import com.mongodb.ServerAddress
import com.mongodb.TransactionOptions
import com.mongodb.client.ClientSession
import com.mongodb.session.ServerSession
import org.bson.BsonDocument
import org.bson.BsonTimestamp
import sun.reflect.generics.reflectiveObjects.NotImplementedException

/**
 * As of 30.08.18 there is no testing
 * framework for mongo that supports the 3.8.1 version
 * of the driver, so the underlying transactions calls
 * are stubbed.
 *
 * @author Tsvetozar Bonev (tsbonev@gmail.com)
 */
class MongoClientSessionStub: ClientSession {

	override fun startTransaction() {
		System.err.println("[mongodb-stub] Starting transaction")
	}

	override fun hasActiveTransaction(): Boolean {
		return false
	}

	override fun commitTransaction() {
		System.err.println("[mongodb-stub] Committing transaction")
	}

	override fun close() {
		System.err.println("[mongodb-stub] Closing session")
	}

	//region unimplemented

	override fun abortTransaction() {
		throw NotImplementedException()
	}

	override fun getOriginator(): Any {
		throw NotImplementedException()
	}

	override fun advanceClusterTime(clusterTime: BsonDocument?) {
		throw NotImplementedException()
	}

	override fun notifyMessageSent(): Boolean {
		throw NotImplementedException()
	}

	override fun getClusterTime(): BsonDocument {
		throw NotImplementedException()
	}

	override fun getOperationTime(): BsonTimestamp {
		throw NotImplementedException()
	}

	override fun getTransactionOptions(): TransactionOptions {
		throw NotImplementedException()
	}

	override fun startTransaction(transactionOptions: TransactionOptions) {
		throw NotImplementedException()
	}

	override fun isCausallyConsistent(): Boolean {
		throw NotImplementedException()
	}

	override fun getOptions(): ClientSessionOptions {
		throw NotImplementedException()
	}

	override fun getServerSession(): ServerSession {
		throw NotImplementedException()
	}

	override fun advanceOperationTime(operationTime: BsonTimestamp?) {
		throw NotImplementedException()
	}

	//endregion  int
}