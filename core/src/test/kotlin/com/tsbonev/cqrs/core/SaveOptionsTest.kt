package com.tsbonev.cqrs.core

import com.tsbonev.cqrs.core.eventstore.SaveOptions
import org.hamcrest.CoreMatchers
import org.junit.Test
import org.hamcrest.CoreMatchers.`is` as Is
import org.junit.Assert.assertThat

/**
 * @author Tsvetozar Bonev (tsbonev@gmail.com)
 */
class SaveOptionsTest {

	@Test
	fun `Aggregate Id is specified`() {
		val saveOptions = SaveOptions(aggregateId = "::id::")

		assertThat(saveOptions.aggregateId, Is(CoreMatchers.equalTo("::id::")))
		assertThat(saveOptions.aggregateId, Is(CoreMatchers.equalTo("::id::")))
	}

	@Test
	fun `Aggregate id is not specified`() {
		val saveOptions = SaveOptions()

		assertThat(saveOptions.aggregateId, Is(CoreMatchers.equalTo(saveOptions.aggregateId)))
	}

	@Test
	fun `Aggregate Id is unique in every instance`() {
		val saveOptions1 = SaveOptions()
		val saveOptions2 = SaveOptions()

		assertThat(saveOptions1.aggregateId, Is(CoreMatchers.not(CoreMatchers.equalTo(saveOptions2.aggregateId))))
	}
}