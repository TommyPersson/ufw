package io.tpersson.ufw.core.utils

import kotlinx.coroutines.*
import org.slf4j.Logger
import java.lang.Exception
import java.time.Duration
import kotlin.reflect.KClass


public suspend fun forever(
    logger: Logger,
    errorDelay: Duration = Duration.ofMillis(500),
    block: suspend () -> Unit
): Unit = coroutineScope {
    while (isActive) {
        try {
            block()
        } catch (e: Exception) {
            if (e is CancellationException) {
                return@coroutineScope
            }

            logger.error("forever: Unhandled exception: $e", e)

            withContext(NonCancellable) {
                delay(errorDelay.toMillis())
            }
        }
    }
}

/**
 * Returns the [KClass.qualifiedName], but the package names are reduced to their first letters.
 *
 * E.g.: "i.t.u.c.CoreComponent"
 */
public val KClass<*>.shortQualifiedName: String?
    get() {
        if (qualifiedName == null || simpleName == null) {
            return null
        }

        val parts = qualifiedName!!.split(".")
        val packages = parts.take(parts.size - 1)
        val firstLetters = packages.map { it.first() }
        return firstLetters.joinToString(".") + "." + simpleName
    }