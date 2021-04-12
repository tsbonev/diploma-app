package com.tsbonev.cqrs.core.testcontracts

import com.google.gson.Gson
import com.tsbonev.cqrs.core.DataModelFormat
import com.tsbonev.cqrs.core.snapshot.MessageFormat
import java.io.InputStream
import java.io.InputStreamReader
import java.lang.reflect.Type

class TestMessageFormat(vararg types: Class<*>) : MessageFormat, DataModelFormat {
	private val gson = Gson()
	private val kindToType = mutableMapOf<String, Class<*>>()

	init {
		types.forEach {
			kindToType[it.simpleName] = it
		}
	}

	override fun supportsKind(kind: String): Boolean {
		return kindToType.containsKey(kind)
	}

	@Suppress("UNCHECKED_CAST")
	override fun <T> parse(stream: InputStream, kind: String): T {
		val type = kindToType.getValue(kind)
		return gson.fromJson(InputStreamReader(stream, Charsets.UTF_8), type) as T
	}

	override fun formatToBytes(value: Any): ByteArray {
		return gson.toJson(value).toByteArray(Charsets.UTF_8)
	}

	override fun <T> parse(stream: InputStream, type: Type): T {
		return gson.fromJson(InputStreamReader(stream, Charsets.UTF_8), type)
	}

	override fun formatToString(value: Any): String {
		return gson.toJson(value)
	}
}