package io.tpersson.ufw.jobqueue

import io.tpersson.ufw.database.unitofwork.UnitOfWork


public interface JobQueue {
    public suspend fun <TJob : Job> enqueue(
        job: TJob,
        unitOfWork: UnitOfWork,
        builder: JobOptionsBuilder.() -> Unit = {}
    )
}
