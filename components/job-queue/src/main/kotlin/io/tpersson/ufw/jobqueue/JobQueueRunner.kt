package io.tpersson.ufw.jobqueue

import io.tpersson.ufw.managed.Managed
import jakarta.inject.Inject

public class JobQueueRunner @Inject constructor(
    private val jobHandlers: JobHandlersProvider
) : Managed() {
    override suspend fun launch() {
        println("launching $jobHandlers")
    }
}