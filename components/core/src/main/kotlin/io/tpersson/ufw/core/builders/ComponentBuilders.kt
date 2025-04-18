package io.tpersson.ufw.core.builders

import kotlin.reflect.KClass
import kotlin.reflect.full.allSupertypes
import kotlin.reflect.full.companionObjectInstance

public class ComponentBuilders : Iterable<ComponentBuilder<Component<Any>>> {

    private val entries = LinkedHashMap<ComponentKey<*>, ComponentBuilder<*>>()

    public fun <K : ComponentKey<C>, V : ComponentBuilder<C>, C : Component<*>> get(key: K): V? {
        @Suppress("UNCHECKED_CAST")
        return entries[key] as V?
    }

    public fun <K : ComponentKey<C>, V : ComponentBuilder<C>, C : Component<*>> add(value: V) {
        @Suppress("UNCHECKED_CAST")
        val key = ( // TODO simplify
                value::class.allSupertypes
                    .first { it.classifier == ComponentBuilder::class }
                    .arguments.get(0)!!
                    .type!!.classifier as KClass<Any>
                ).companionObjectInstance as K?

        entries[key!!] = value
    }

    override fun iterator(): Iterator<ComponentBuilder<Component<Any>>> {
        @Suppress("UNCHECKED_CAST")
        return entries.values.iterator() as Iterator<ComponentBuilder<Component<Any>>>
    }
}