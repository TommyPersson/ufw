package io.tpersson.ufw.core.utils

import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

public class Memoized<TDependency : Any, TValue : Any>(
    private val dependency: () -> TDependency,
    private val factory: (TDependency) -> TValue
) : ReadOnlyProperty<Any, TValue> {

    private val lock = Any()
    private var previous: TDependency? = null

    private var value: TValue? = null

    override fun getValue(thisRef: Any, property: KProperty<*>): TValue {
        val current = dependency()

        return synchronized(lock) {
            if (current != previous) {
                value = factory(current)
                previous = current
            }

            value!!
        }
    }
}