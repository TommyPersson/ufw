package io.tpersson.ufw.jobqueue

import io.tpersson.ufw.database.unitofwork.UnitOfWork
import java.time.Duration


public interface JobQueue {
    public suspend fun <TJob : Job> enqueue(
        job: TJob,
        unitOfWork: UnitOfWork,
        builder: JobOptionsBuilder.() -> Unit = {}
    )
}

public data class JobQueueModuleConfig(
    val pollWaitTime: Duration = Duration.ofSeconds(5),
    val defaultJobTimeout: Duration = Duration.ofMinutes(10),
    val defaultJobRetention: Duration = Duration.ofDays(14),
) {
    public companion object {
        public val Default: JobQueueModuleConfig = JobQueueModuleConfig()
    }
}