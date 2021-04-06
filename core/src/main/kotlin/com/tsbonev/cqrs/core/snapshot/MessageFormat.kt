package com.tsbonev.cqrs.core.snapshot

import java.io.InputStream

/**
 * @author Tsvetozar Bonev (tsbonev@gmail.com)
 */
interface MessageFormat {

	/**
	 * Ensures that the provided kind could be supported.
	 */
	fun supportsKind(kind: String): Boolean

	/**
	 * Parses JSON content from the provided input stream.
	 */
	fun <T> parse(stream: InputStream, kind: String): T

	/**
	 * Formats the provided value into binary value.
	 */
	fun formatToBytes(value: Any): ByteArray
}