package com.tsbonev.cqrs.core.snapshot

import com.tsbonev.cqrs.core.BinaryPayload

/**
 * @author Tsvetozar Bonev (tsbonev@gmail.com)
 */
data class Snapshot(val version: Long, val data: BinaryPayload)