package io.tpersson.ufw.jobqueue

import io.tpersson.ufw.database.unitofwork.UnitOfWork
import io.tpersson.ufw.jobqueue.v2.DurableJob


public interface JobQueue {
    public suspend fun <TJob : Job> enqueue(
        job: TJob,
        unitOfWork: UnitOfWork,
        builder: JobOptionsBuilder.() -> Unit = {}
    )

    public suspend fun <TJob : DurableJob> enqueue(
        job: TJob,
        unitOfWork: UnitOfWork,
        builder: JobOptionsBuilder.() -> Unit = {}
    )
}
