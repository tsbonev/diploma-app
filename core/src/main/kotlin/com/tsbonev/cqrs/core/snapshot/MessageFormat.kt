package com.tsbonev.cqrs.core.snapshot

interface MessageFormat<V> {

	/**
	 * Ensures that the provided kind could be supported.
	 */
	fun supportsKind(kind: String): Boolean

	/**
	 * Parses JSON content from the provided input stream.
	 */
	fun <T> parse(value: V, kind: String): T

	/**
	 * Formats the provided value into binary value.
	 */
	fun format(value: Any): V
}