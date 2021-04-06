package com.tsbonev.cqrs.core

import java.time.LocalDateTime

/**
 * @author Tsvetozar Bonev (tsbonev@gmail.com)
 */
object IdGenerators {

	/**
	 * Creates a new Distributed Sequence Generator inspired by Twitter snowflake:
	 * https://github.com/twitter/snowflake/tree/snowflake-2010
	 *
	 * **Note:** A single instance of the sequence generator needs to be used.
	 *
	 * @param nodeId the nodeId which will be used as disciminator. A nodeId will be generated automatically if none is provided.
	 * @param epoch the epoch time which will be used
	 */
	fun snowflake(nodeId: String? = null, epoch: LocalDateTime = LocalDateTime.of(2015, 1, 1, 0, 0, 0)): IdGenerator {
		if (nodeId != null) {
			return SnowflakeIdGenerator(Math.abs(nodeId.hashCode() % 1023), epoch)
		}
		return SnowflakeIdGenerator()
	}
}