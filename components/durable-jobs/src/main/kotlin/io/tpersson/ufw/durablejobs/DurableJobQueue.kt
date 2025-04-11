package io.tpersson.ufw.durablejobs

import io.tpersson.ufw.database.unitofwork.UnitOfWork


public interface DurableJobQueue {
    public suspend fun <TJob : DurableJob> enqueue(
        job: TJob,
        unitOfWork: UnitOfWork,
        builder: DurableJobOptionsBuilder.() -> Unit = {}
    )

    // TODO cancellation
}
