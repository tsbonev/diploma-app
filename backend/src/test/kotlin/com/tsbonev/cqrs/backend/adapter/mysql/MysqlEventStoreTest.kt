package com.tsbonev.cqrs.backend.adapter.mysql

import com.tsbonev.cqrs.core.contracts.EventStoreContract
import com.tsbonev.cqrs.core.eventstore.EventStore
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest

@DataJpaTest
class MysqlEventStoreTest @Autowired constructor(val repo: MysqlEventRepository): EventStoreContract() {

	override fun createEventStore(): EventStore {
		return MysqlEventStore(repo)
	}
}