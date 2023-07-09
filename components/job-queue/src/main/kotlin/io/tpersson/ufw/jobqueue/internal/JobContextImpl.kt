package io.tpersson.ufw.jobqueue.internal

import io.tpersson.ufw.database.unitofwork.UnitOfWork
import io.tpersson.ufw.jobqueue.JobContext

public class JobContextImpl(
    override val unitOfWork: UnitOfWork
) : JobContext