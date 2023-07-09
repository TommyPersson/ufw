package io.tpersson.ufw.jobqueue

import io.tpersson.ufw.database.unitofwork.UnitOfWork

public interface JobContext {
    public val unitOfWork: UnitOfWork
}

