package io.tpersson.ufw.managed

import kotlinx.coroutines.*
import kotlinx.coroutines.slf4j.MDCContext
import org.slf4j.MDC
import kotlin.coroutines.CoroutineContext

public abstract class ManagedJob(
    context: CoroutineContext = Dispatchers.Default
) : Managed() {
    private val scope = CoroutineScope(context + SupervisorJob())
    private var job: Job? = null

    protected val isActive: Boolean get() = job?.isActive == true

    override suspend fun onStarted() {
        MDC.put("managedType", this::class.simpleName)

        job = scope.launch(MDCContext()) {
            launch()
        }
    }

    override suspend fun onStopped() {
        job?.cancelAndJoin()
    }

    protected abstract suspend fun launch(): Unit
}

