package io.tpersson.ufw.jobqueue.internal

import io.tpersson.ufw.database.unitofwork.UnitOfWork

public interface JobFailureRepository {
    public suspend fun getLatestFor(job: InternalJob<*>, limit: Int): List<JobFailure>

    public suspend fun getNumberOfFailuresFor(job: InternalJob<*>): Int

    public fun insert(failure: JobFailure, unitOfWork: UnitOfWork)
}

