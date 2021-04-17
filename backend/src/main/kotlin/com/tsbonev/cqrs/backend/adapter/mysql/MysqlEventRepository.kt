package com.tsbonev.cqrs.backend.adapter.mysql

import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository
import javax.persistence.Entity
import javax.persistence.Id

@Repository
interface MysqlEventRepository : CrudRepository<MysqlEvent, String>

@Entity
data class MysqlEvent(
	@Id val eventId: String, val version: Long = 0, val payload: String
)