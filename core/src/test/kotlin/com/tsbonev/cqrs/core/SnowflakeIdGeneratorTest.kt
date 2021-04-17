package com.tsbonev.cqrs.core

import com.tsbonev.cqrs.core.contracts.IdGeneratorContract
import org.junit.jupiter.api.Test
import org.hamcrest.CoreMatchers.`is` as Is
import org.hamcrest.MatcherAssert.assertThat


class SnowflakeIdGeneratorTest : IdGeneratorContract() {

	@Test
	fun `Accepts custom node id`() {
		val generator = IdGenerators.snowflake("node1")
		val id1 = generator.nextId()
		val id2 = generator.nextId()
		assertThat(id1 < id2, Is(true))
	}

	override fun createSequenceGenerator(): IdGenerator {
		return IdGenerators.snowflake()
	}
}