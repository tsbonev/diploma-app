package com.tsbonev.cqrs.core

import java.time.Instant

/**
 * @author Tsvetozar Bonev (tsbonev@gmail.com)
 */
data class Identity(val id: String, val tenant: String, val time: Instant)