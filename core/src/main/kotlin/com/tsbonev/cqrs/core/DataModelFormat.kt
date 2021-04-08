package com.tsbonev.cqrs.core

import java.io.InputStream
import java.lang.reflect.Type


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