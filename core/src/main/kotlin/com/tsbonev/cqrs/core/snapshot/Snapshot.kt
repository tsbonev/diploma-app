package com.tsbonev.cqrs.core.snapshot

import com.tsbonev.cqrs.core.BinaryPayload


data class Snapshot(val version: Long, val data: BinaryPayload)