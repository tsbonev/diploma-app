package com.tsbonev.cqrs.core.messagebus


interface Validator<in T> {
	fun validate(fieldValue: T): Boolean
}