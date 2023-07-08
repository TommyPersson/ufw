package io.tpersson.ufw.jobqueue.internal

import io.tpersson.ufw.managed.Managed
import kotlinx.coroutines.*


public class JobQueueWorker(
    private val jobQueue: JobQueueInternal
) : Managed() {
    suspend override fun launch(): Unit = coroutineScope {
        while (isActive) {

        }
    }
}

