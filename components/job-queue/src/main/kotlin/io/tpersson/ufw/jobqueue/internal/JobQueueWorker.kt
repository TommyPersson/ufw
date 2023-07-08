package io.tpersson.ufw.jobqueue.internal

import io.tpersson.ufw.managed.Managed
import jakarta.inject.Inject
import kotlinx.coroutines.*


public class JobQueueWorker @Inject constructor(
    private val jobQueue: JobQueueInternal
) /*: Managed()*/ {
    public suspend /*override*/ fun launch(): Unit = coroutineScope {
        while (isActive) {
            delay(500)
        }
    }
}

