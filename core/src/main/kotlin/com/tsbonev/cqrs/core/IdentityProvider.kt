package com.tsbonev.cqrs.core

interface IdentityProvider {
	/**
	 * Get returns the identity associated with the request.
	 */
	fun get(): Identity
}