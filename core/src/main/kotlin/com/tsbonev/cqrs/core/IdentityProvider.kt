package com.tsbonev.cqrs.core

import java.time.Instant


interface IdentityProvider {

	/**
	 * Get returns the identity associated with the request.
	 */
	fun get(): Identity


	class Default : IdentityProvider {
		override fun get(): Identity {
			return Identity("-1", "default", Instant.now())
		}
	}
}