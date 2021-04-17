package com.tsbonev.cqrs.backend.adapter.mysql

import com.tsbonev.cqrs.core.eventstore.ConcreteAggregate
import com.tsbonev.cqrs.core.eventstore.EventStore
import com.tsbonev.cqrs.core.eventstore.GetAllEventsRequest
import com.tsbonev.cqrs.core.eventstore.GetAllEventsResponse
import com.tsbonev.cqrs.core.eventstore.GetEventsFromStreamsRequest
import com.tsbonev.cqrs.core.eventstore.GetEventsResponse
import com.tsbonev.cqrs.core.eventstore.ReadDirection
import com.tsbonev.cqrs.core.eventstore.RevertEventsResponse
import com.tsbonev.cqrs.core.eventstore.SaveEventsRequest
import com.tsbonev.cqrs.core.eventstore.SaveEventsResponse
import com.tsbonev.cqrs.core.eventstore.SaveOptions
import org.springframework.beans.factory.annotation.Autowired

class MysqlEventStore(@Autowired val mysqlEventRepository: MysqlEventRepository) : EventStore {

	override fun saveEvents(request: SaveEventsRequest, saveOptions: SaveOptions): SaveEventsResponse {
		return SaveEventsResponse.Success(1L , listOf(), ConcreteAggregate("a", null, 1, listOf()))
	}

	override fun getEventsFromStreams(request: GetEventsFromStreamsRequest): GetEventsResponse {
		return GetEventsResponse.Success(listOf())
	}

	override fun getAllEvents(request: GetAllEventsRequest): GetAllEventsResponse {
		return GetAllEventsResponse.Success(listOf(), ReadDirection.FORWARD, null)
	}

	override fun revertEvents(tenant: String, stream: String, count: Int): RevertEventsResponse {
		return RevertEventsResponse.Success(listOf())
	}
}