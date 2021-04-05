package com.tsbonev.cqrs.core.messagebus

import com.tsbonev.cqrs.core.EventWithBinaryPayload

/**
 * @author Tsvetozar Bonev (tsbonev@gmail.com)
 */
interface Interceptor {

	interface Chain {

		fun event(): EventWithBinaryPayload

		fun proceed(event: EventWithBinaryPayload)

	}

	fun intercept(chain: Chain)
}