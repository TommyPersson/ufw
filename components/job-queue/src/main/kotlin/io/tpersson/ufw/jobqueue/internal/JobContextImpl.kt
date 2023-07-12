package io.tpersson.ufw.jobqueue.internal

import io.tpersson.ufw.database.unitofwork.UnitOfWork
import io.tpersson.ufw.database.unitofwork.UnitOfWorkFactory
import io.tpersson.ufw.jobqueue.JobContext
import java.time.Instant
import java.time.InstantSource

public class JobContextImpl(
    override val unitOfWork: UnitOfWork
) : JobContext