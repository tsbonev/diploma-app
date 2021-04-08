package com.tsbonev.cqrs.core


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