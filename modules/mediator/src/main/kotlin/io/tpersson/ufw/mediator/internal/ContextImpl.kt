package io.tpersson.ufw.mediator.internal

import io.tpersson.ufw.mediator.Context
import io.tpersson.ufw.mediator.ContextKey

public class ContextImpl : Context {
    private val backingMap = mutableMapOf<ContextKey<*>, Any>()

    override fun <T : Any> set(key: ContextKey<T>, value: T) {
        backingMap[key] = value
    }

    override fun <T : Any> get(key: ContextKey<T>): T? {
        @Suppress("UNCHECKED_CAST")
        return backingMap[key] as T?
    }

    override fun toMap(): Map<ContextKey<*>, Any> {
        return backingMap.toMap()
    }
}