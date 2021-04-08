package com.tsbonev.cqrs.core.messagebus

import com.tsbonev.cqrs.core.EventWithBinaryPayload


interface Interceptor {

	interface Chain {

		fun event(): EventWithBinaryPayload

		fun proceed(event: EventWithBinaryPayload)

	}

	fun intercept(chain: Chain)
}