package io.tpersson.ufw.managed

import kotlinx.coroutines.*
import kotlin.coroutines.CoroutineContext

public abstract class Managed(
    context: CoroutineContext = Dispatchers.Default
) {
    private val scope = CoroutineScope(context)
    private lateinit var job: Job

    public fun start() {
        job = scope.launch {
            launch()
        }
    }

    public suspend fun stop() {
        job.cancelAndJoin()
    }

    public abstract suspend fun launch(): Unit

}