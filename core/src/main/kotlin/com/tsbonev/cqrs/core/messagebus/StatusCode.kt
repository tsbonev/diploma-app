package com.tsbonev.nharker.cqrs

/**
 * Http status codes taken from ktor's status code class to
 * decouple the responses from the framework completely.
 *
 * @author Tsvetozar Bonev (tsbonev@gmail.com)
 */
@Suppress("unused")
data class StatusCode(val value: Int, val description: String) {
	companion object {
		val Continue = StatusCode(100, "Continue")
		val SwitchingProtocols = StatusCode(101, "Switching Protocols")
		val Processing = StatusCode(102, "Processing")

		val OK = StatusCode(200, "OK")
		val Created = StatusCode(201, "Created")
		val Accepted = StatusCode(202, "Accepted")
		val NonAuthoritativeInformation = StatusCode(203, "Non-Authoritative Information")
		val NoContent = StatusCode(204, "No Content")
		val ResetContent = StatusCode(205, "Reset Content")
		val PartialContent = StatusCode(206, "Partial Content")
		val MultiStatus = StatusCode(207, "Multi-Status")

		val MultipleChoices = StatusCode(300, "Multiple Choices")
		val MovedPermanently = StatusCode(301, "Moved Permanently")
		val Found = StatusCode(302, "Found")
		val SeeOther = StatusCode(303, "See Other")
		val NotModified = StatusCode(304, "Not Modified")
		val UseProxy = StatusCode(305, "Use Proxy")
		val SwitchProxy = StatusCode(306, "Switch Proxy")
		val TemporaryRedirect = StatusCode(307, "Temporary Redirect")
		val PermanentRedirect = StatusCode(308, "Permanent Redirect")

		val BadRequest = StatusCode(400, "Bad Request")
		val Unauthorized = StatusCode(401, "Unauthorized")
		val PaymentRequired = StatusCode(402, "Payment Required")
		val Forbidden = StatusCode(403, "Forbidden")
		val NotFound = StatusCode(404, "Not Found")
		val MethodNotAllowed = StatusCode(405, "Method Not Allowed")
		val NotAcceptable = StatusCode(406, "Not Acceptable")
		val ProxyAuthenticationRequired = StatusCode(407, "Proxy Authentication Required")
		val RequestTimeout = StatusCode(408, "Request Timeout")
		val Conflict = StatusCode(409, "Conflict")
		val Gone = StatusCode(410, "Gone")
		val LengthRequired = StatusCode(411, "Length Required")
		val PreconditionFailed = StatusCode(412, "Precondition Failed")
		val PayloadTooLarge = StatusCode(413, "Payload Too Large")
		val RequestURITooLong = StatusCode(414, "Request-URI Too Long")

		val UnsupportedMediaType = StatusCode(415, "Unsupported Media Type")
		val RequestedRangeNotSatisfiable = StatusCode(416, "Requested Range Not Satisfiable")
		val ExpectationFailed = StatusCode(417, "Expectation Failed")
		val UnprocessableEntity = StatusCode(422, "Unprocessable Entity")
		val Locked = StatusCode(423, "Locked")
		val FailedDependency = StatusCode(424, "Failed Dependency")
		val UpgradeRequired = StatusCode(426, "Upgrade Required")
		val TooManyRequests = StatusCode(429, "Too Many Requests")
		val RequestHeaderFieldTooLarge = StatusCode(431, "Request Header Fields Too Large")

		val InternalServerError = StatusCode(500, "Internal Server Error")
		val NotImplemented = StatusCode(501, "Not Implemented")
		val BadGateway = StatusCode(502, "Bad Gateway")
		val ServiceUnavailable = StatusCode(503, "Service Unavailable")
		val GatewayTimeout = StatusCode(504, "Gateway Timeout")
		val VersionNotSupported = StatusCode(505, "HTTP Version Not Supported")
		val VariantAlsoNegotiates = StatusCode(506, "Variant Also Negotiates")
		val InsufficientStorage = StatusCode(507, "Insufficient Storage")
	}
}

@Suppress("unused")
fun StatusCode.isSuccess(): Boolean = value in (200 until 300)

@Suppress("unused")
fun StatusCode.isUserError(): Boolean = value in (400 until 500)

@Suppress("unused")
fun StatusCode.isInternalError(): Boolean = value in (500 until 600)