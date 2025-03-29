package io.tpersson.ufw.mediator.internal

import io.tpersson.ufw.mediator.Mediator
import io.tpersson.ufw.mediator.Request
import kotlin.reflect.KClass

public interface MediatorInternal : Mediator {
    public val requestClasses: List<KClass<out Request<*>>>
}