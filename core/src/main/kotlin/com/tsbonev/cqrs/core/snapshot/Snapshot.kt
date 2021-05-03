package com.tsbonev.cqrs.core.snapshot

data class Snapshot(val version: Long, val data: Any)