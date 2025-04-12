package io.tpersson.ufw.core.utils

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KClass

public object LoggerCache {

    private val cache = ConcurrentHashMap<String, Logger>()

    public fun get(name: String): Logger = cache.getOrPut(name) { LoggerFactory.getLogger(name) }

    public fun get(clazz: Class<*>): Logger = get(clazz.name)

    public fun get(klazz: KClass<*>): Logger = get(klazz.java)
}