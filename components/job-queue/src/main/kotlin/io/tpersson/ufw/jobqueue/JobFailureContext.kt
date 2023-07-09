package io.tpersson.ufw.jobqueue

import io.tpersson.ufw.database.unitofwork.UnitOfWork

public interface JobFailureContext {
    public val numberOfFailures: Int
    public val unitOfWork: UnitOfWork
}