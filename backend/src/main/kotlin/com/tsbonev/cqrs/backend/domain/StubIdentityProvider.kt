package com.tsbonev.cqrs.backend.domain

import com.tsbonev.cqrs.core.Identity
import com.tsbonev.cqrs.core.IdentityProvider
import org.springframework.stereotype.Component
import java.time.Instant

@Component
class StubIdentityProvider : IdentityProvider {
	override fun get(): Identity {
		return Identity("::stubId:", Instant.now().toEpochMilli())
	}
}