package com.tsbonev.cqrs.core

import java.time.Instant


data class Identity(val id: String, val tenant: String, val time: Instant)