package io.tpersson.ufw.managed

import jakarta.inject.Inject
import kotlinx.coroutines.*
import kotlin.concurrent.thread

public class ManagedRunner @Inject constructor(
    private val instances: Set<Managed>
) {
    init {
        instances.forEach {
            it.start()
        }

        Runtime.getRuntime().addShutdownHook {
            val jobs = instances.map { async { it.stop() } }
            jobs.awaitAll()
        }
    }
}

private fun Runtime.addShutdownHook(block: suspend CoroutineScope.() -> Unit) {
    addShutdownHook(thread(false) {
        runBlocking {
            coroutineScope {
                block()
            }
        }
    })
}
