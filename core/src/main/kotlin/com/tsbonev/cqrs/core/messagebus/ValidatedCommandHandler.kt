package com.tsbonev.cqrs.core.messagebus

import com.tsbonev.cqrs.core.Command
import com.tsbonev.cqrs.core.CommandHandler


data class ValidatedCommandHandler<in T : Command<R>, R>(val handler: CommandHandler<T, R>, val validation: Validation<T>)