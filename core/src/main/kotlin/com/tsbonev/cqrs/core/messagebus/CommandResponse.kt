package com.tsbonev.cqrs.core.messagebus

import com.tsbonev.nharker.cqrs.StatusCode
import java.util.Optional

typealias QueryResponse = CommandResponse

/**
 * An object that Command Handlers return to clue in the caller
 * what has happened with their command.
 *
 * @author Tsvetozar Bonev (tsbonev@gmail.com)
 */
data class CommandResponse(
	val statusCode: StatusCode,
	val rawPayload: Any? = null
) {
	val payload: Optional<Any> =
		if (rawPayload == null) Optional.empty()
		else Optional.of(rawPayload)
}