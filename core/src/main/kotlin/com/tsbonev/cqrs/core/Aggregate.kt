package com.tsbonev.cqrs.core

/**
 * @author Tsvetozar Bonev (tsbonev@gmail.com)
 */

/**
 * Aggregate interface
 */
interface Aggregate {

	/**
	 * Returns the Id of the Aggregate
	 *
	 * @return Aggregate Id
	 */
	fun getId(): String

	fun commitChanges()

	fun getChanges(): List<Any>
}