package com.tsbonev.cqrs.core


data class BinaryPayload(val payload: ByteArray) {
	constructor(payload: String) : this(payload.toByteArray(Charsets.UTF_8))

	override fun equals(other: Any?): Boolean {
		if (this === other) return true
		if (javaClass != other?.javaClass) return false

		other as BinaryPayload

		if (!payload.contentEquals(other.payload)) return false

		return true
	}

	override fun hashCode(): Int {
		return payload.contentHashCode()
	}
}