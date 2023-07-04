package io.tpersson.ufw.core.utils

import kotlin.reflect.KClass
import kotlin.reflect.KProperty1
import kotlin.reflect.full.declaredMemberProperties

public fun <T : Any?> valueByPath(path: String, obj: Any): Pair<KClass<out T & Any>, T> {
    return getValueByPath(path, obj) as Pair<KClass<out T & Any>, T>
}

// TODO do some smarter reflection or LambdaMetaFactory stuff later
private fun getValueByPath(path: String, obj: Any): Pair<KClass<Any>, Any?> {
    return if (!path.contains(".")) {
        getValueForSimpleArgument(path, obj)
    } else {
        getValueForNestedArgument(path, obj)
    }
}

private fun getValueForSimpleArgument(argName: String, obj: Any): Pair<KClass<Any>, Any?> {
    @Suppress("UNCHECKED_CAST")
    val property = obj::class.declaredMemberProperties
        .map { it as KProperty1<Any, Any?> }
        .firstOrNull { it.name == argName }
        ?: error("Unable to find parameter for '$argName'")

    val propertyType = property.returnType.classifier as KClass<Any>

    return propertyType to property.get(obj)
}

private fun getValueForNestedArgument(argName: String, root: Any): Pair<KClass<Any>, Any?> {
    val fullPath = argName.split(".")
    var path = fullPath[0]

    var (type, obj) = getValueForSimpleArgument(path, root)
    if (obj == null) {
        return type to null
    }

    var i = 1
    while (i < fullPath.size) {
        path = fullPath[i]

        @Suppress("UNCHECKED_CAST")
        val property = obj!!::class.declaredMemberProperties.firstOrNull { it.name == path } as? KProperty1<Any, Any?>
            ?: error("Unable to find parameter for '$argName'")

        type = property.returnType.classifier as KClass<Any>

        val propertyValue = property.get(obj)
            ?: return type to null

        obj = propertyValue
        i++
    }

    return type to obj
}