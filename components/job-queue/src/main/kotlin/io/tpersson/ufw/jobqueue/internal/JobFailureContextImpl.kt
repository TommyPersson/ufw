package io.tpersson.ufw.jobqueue.internal

import io.tpersson.ufw.database.unitofwork.UnitOfWork
import io.tpersson.ufw.jobqueue.JobFailureContext

public class JobFailureContextImpl(
    override val numberOfFailures: Int,
    override val unitOfWork: UnitOfWork,
) : JobFailureContext