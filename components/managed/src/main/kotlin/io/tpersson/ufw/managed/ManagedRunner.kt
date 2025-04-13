package io.tpersson.ufw.managed

import io.tpersson.ufw.core.logging.createLogger
import io.tpersson.ufw.core.utils.forever
import jakarta.inject.Inject
import kotlinx.coroutines.*
import java.time.Duration
import kotlin.concurrent.thread

public class ManagedRunner @Inject constructor(
    _instances: Set<Managed>
) {
    private val logger = createLogger()

    private val instances = _instances.toMutableSet()

    public fun register(instance: Managed) {
        instances.add(instance)
    }

    public fun startAll(addShutdownHook: Boolean = true) {
        runBlocking {
            instances.forEach {
                it.start()
            }
        }

        if (addShutdownHook) {
            Runtime.getRuntime().addShutdownHook(thread(false) {
                runBlocking {
                    stopAll()
                }
            })
        }
    }

    public suspend fun stopAll(): Unit = coroutineScope {
        val asdf = launch {
            forever(logger, interval = Duration.ofSeconds(5)) {
                logger.info("Waiting for instances: ${instances.filter { it.isRunning }.map { it::class.simpleName }} ...")
            }
        }

        val jobs = instances.map {
            async {
                it.stop()
            }
        }

        jobs.awaitAll()
        asdf.cancelAndJoin()
    }
}

