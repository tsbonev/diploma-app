package com.tsbonev.cqrs.core

import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method

/**
 * @author Tsvetozar Bonev (tsbonev@gmail.com)
 */
abstract class AggregateBase private constructor(protected var aggregateId: String = "", protected var version: Long = 0L): Aggregate {
	private val mutations: ArrayList<Any> = arrayListOf();

	constructor(): this("")

	override fun getId(): String {
		return aggregateId
	}

	override fun commitChanges() {
		mutations.clear()
	}

	override fun getChanges(): List<Any> {
		return mutations
	}

	/**
	 * @param event
	 * @param isNew
	 */
	private fun mutateState(event: Any, isNew: Boolean = true) {
		var method: Method? = null

		try {
			method = this::class.java.getDeclaredMethod("apply", event::class.java)
		} catch (e: NoSuchMethodException) {
			/**
			 * Events do not necessarily mutate the state.
			 */
		}

		if (method != null) {
			method.isAccessible = true
			try {
				method.invoke(this, event)
			} catch (e: IllegalAccessException) {
				throw IllegalStateException(e)
			} catch (e: IllegalArgumentException) {
				throw IllegalStateException(e)
			} catch (e: InvocationTargetException) {
				throw IllegalStateException(e)
			}
		}

		if (isNew) {
			mutations.add(event)
		}
	}


	/**
	 * Sets up the state after a snapshot.
	 */
	private fun setupState(donor: Any, recipient: Any) {
		donor.javaClass.declaredFields.forEach { field ->
			try {
				val donorField = donor.javaClass.getDeclaredField(field.name)
				donorField.isAccessible = true
				val value = donorField.get(donor)
				val declaredField = recipient.javaClass.getDeclaredField(field.name)
				declaredField.isAccessible = true
				declaredField.set(recipient, value)
			} catch (e: IllegalAccessException) {
				throw IllegalStateException(e)
			} catch (e: NoSuchFieldException) {
				throw IllegalStateException(e)
			}
		}

		val aggregateIdFieldDonor = donor.javaClass.superclass.getDeclaredField("aggregateId")
		aggregateIdFieldDonor.isAccessible = true
		val value = aggregateIdFieldDonor.get(donor)
		val declaredField = recipient.javaClass.superclass.getDeclaredField("aggregateId")
		declaredField.isAccessible = true
		declaredField.set(recipient, value)
	}
}