package io.tpersson.ufw.managed

import jakarta.inject.Inject
import kotlinx.coroutines.*
import kotlin.concurrent.thread

public class ManagedRunner @Inject constructor(
    private val instances: Set<Managed>
) {
    public fun startAll() {
        instances.forEach {
            it.start()
        }
    }

    public suspend fun stopAll(): Unit = coroutineScope {
        val jobs = instances.map { async { it.stop() } }
        jobs.awaitAll()
    }
}

