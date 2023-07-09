package io.tpersson.ufw.managed

import kotlinx.coroutines.*
import kotlin.coroutines.CoroutineContext

public abstract class Managed(
    context: CoroutineContext = Dispatchers.Default
) {
    private val scope = CoroutineScope(context + SupervisorJob())
    private var job: Job? = null

    public fun start() {
        job = scope.launch {
            launch()
        }
    }

    public suspend fun stop() {
        job?.cancelAndJoin()
    }

    protected abstract suspend fun launch(): Unit

}