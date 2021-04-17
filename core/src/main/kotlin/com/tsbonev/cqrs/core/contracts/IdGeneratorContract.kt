package com.tsbonev.cqrs.core.contracts

import com.tsbonev.cqrs.core.IdGenerator
import org.hamcrest.CoreMatchers
import org.junit.jupiter.api.Test
import org.hamcrest.CoreMatchers.`is` as Is
import org.hamcrest.MatcherAssert.assertThat

abstract class IdGeneratorContract {

	@Test
	fun `Generates sequential ids`() {
		val generator = createSequenceGenerator()
		val id1 = generator.nextId()
		val id2 = generator.nextId()
		assertThat(id1 < id2, Is(true))
	}

	@Test
	fun `Generates multiple ids`() {
		val generator = createSequenceGenerator()

		val ids = mutableListOf<Long>()
		for (i in 1..10) {
			ids.add(generator.nextId())
		}

		val ascComparator = naturalOrder<Long>()
		val sortedIds = ids.toMutableList().sortedWith(ascComparator)

		assertThat(ids, Is(CoreMatchers.equalTo(sortedIds)))
	}

	@Test
	fun `Generates bulk ids`() {
		val generator = createSequenceGenerator()
		val ids = generator.nextIds(10)

		assertThat(ids.size, Is(CoreMatchers.equalTo(10)))
	}

	abstract fun createSequenceGenerator(): IdGenerator
}