package com.tsbonev.cqrs.core


interface EventHandler<in T> {
	fun handle(event: T)
}