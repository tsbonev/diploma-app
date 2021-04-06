package com.tsbonev.cqrs.core

import org.hamcrest.CoreMatchers
import org.junit.Test
import org.hamcrest.CoreMatchers.`is` as Is
import org.junit.Assert.assertThat

/**
 * @author Tsvetozar Bonev (tsbonev@gmail.com)
 */
class SnowflakeIdGeneratorTest {

	@Test
	fun `Ids are generated in a sequence`() {
		val generator = IdGenerators.snowflake()
		val id1 = generator.nextId()
		val id2 = generator.nextId()
		assertThat(id1 < id2, Is(true))
	}

	@Test
	fun `Generate few ids in succession`() {
		val generator = IdGenerators.snowflake()

		val ids = mutableListOf<Long>()
		for (i in 1..10) {
			ids.add(generator.nextId())
		}

		val ascComparator = naturalOrder<Long>()
		val sortedIds = ids.toMutableList().sortedWith(ascComparator)

		assertThat(ids, Is(CoreMatchers.equalTo(sortedIds)))
	}

	@Test
	fun `Generate a bulk of ids`() {
		val generator = IdGenerators.snowflake()
		val ids = generator.nextIds(10)

		assertThat(ids.size, Is(CoreMatchers.equalTo(10)))
	}


	@Test
	fun `Custom node id`() {
		val generator = IdGenerators.snowflake("node1")
		val id1 = generator.nextId()
		val id2 = generator.nextId()
		assertThat(id1 < id2, Is(true))
	}
}