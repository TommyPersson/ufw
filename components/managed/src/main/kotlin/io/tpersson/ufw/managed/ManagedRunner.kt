package io.tpersson.ufw.managed

import jakarta.inject.Inject
import kotlinx.coroutines.*
import kotlin.concurrent.thread

public class ManagedRunner @Inject constructor(
    private val instances: Set<Managed>
) {
    public fun startAll(addShutdownHook: Boolean = true) {
        instances.forEach {
            it.start()
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
        val jobs = instances.map { async { it.stop() } }
        jobs.awaitAll()
    }
}

