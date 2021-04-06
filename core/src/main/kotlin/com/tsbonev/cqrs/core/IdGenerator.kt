package com.tsbonev.cqrs.core

/**
 * @author Tsvetozar Bonev (tsbonev@gmail.com)
 */
interface IdGenerator {
	/**
	 * Generates a new id for the sequence.
	 */
	fun nextId(): Long

	/**
	 * Generates a number of sequence ids.
	 * @param size the number of ids
	 */
	fun nextIds(size: Int): List<Long>
}