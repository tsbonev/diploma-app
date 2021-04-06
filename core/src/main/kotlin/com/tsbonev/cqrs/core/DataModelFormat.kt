package com.tsbonev.cqrs.core

import java.io.InputStream
import java.lang.reflect.Type

/**
 * @author Tsvetozar Bonev (tsbonev@gmail.com)
 */
interface DataModelFormat {
	/**
	 * Parses JSON content from the provided input stream.
	 */
	fun <T> parse(stream: InputStream, type: Type): T

	/**
	 * Formats the provided value into string value.
	 */
	fun formatToString(value: Any): String
}