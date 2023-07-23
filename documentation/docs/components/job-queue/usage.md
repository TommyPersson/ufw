---
title: Usage
---

# Job Queue: Usage

## Defining jobs

You define jobs by first creating class implementing the `Job` interface, and implementing a handler extending
the `JobHandler<T>` base class.

```kotlin title="MyJob.kt" linenums="1"

data class MyJob(
    val myInput: String,
    override val jobId: JobId = JobId.new()
) : Job()

class MyJobHandler : JobHandler<MyJob>() {
    suspend fun handle(job: MyJob, context: JobContext): Unit {
        /* ... */
    }
}
```

## Scheduling jobs

You schedule a job by using the `JobQueue.enqueue`-function. You can access an instance of the `JobQueue` interface from
the `JobQueueComponent`.

```kotlin title="MyScheduler.kt" linenums="1"
val ufw = UFW.build { /* ... */ }
val unitOfWorkFactory = ufw.database.unitOfWorkFactory
val jobQueue: JobQueue = ufw.jobQueue.jobQueue

unitOfWorkFactory.use { uow ->
    jobQueue.enqueue(MyJob(myInput = "Hello, World!"), unitOfWork = uow)
}
```

!!! note "Note: Omitting `UnitOfWork`"

    You may omit the `UnitOfWork` parameter if you don't need perform any other transactional activities.

!!! note "Note: Guice"

    When using Guice, the `JobQueue` instance is available for injection using the `Injector` or `@Inject`.

## Transactionality

The `JobContext` parameter in your job handler contains a `UnitOfWork` instance that will only commit if your handler
finishes successfully. You should use it to perform any transactional activities within the handler.

## Handling errors

You can override the `JobHandler.onFailure` method to implement custom failure handling. The function returns
a `FailureAction`, determining how the job queue should proceed with the job.

Possible `FailureAction`s are:
 
* `FailureAction.GiveUp`: Mark the job as failed, do not retry it automatically. This is the default.
* `FailureAction.RescheduleNow`: Reschedule the job immediately by returning it to the queue.
* `FailureAction.RescheduleAt(Instant)`: Reschedule the job at a specific time by returning it to the queue.

```kotlin title="MyJob.kt" hl_lines="6-12" linenums="1"
class MyJobHandler : JobHandler<MyJob>() {
    suspend fun handle(job: MyJob, context: JobContext): Unit {
        /* ... */
    }

    override suspend fun onFailure(job: MyJob, error: Exception, context: JobFailureContext): FailureAction {
        return if (context.numberOfFailures > 5) {
            FailureAction.GiveUp
        } else {
            FailureAction.RescheduleNow
        }
    }
}
```

The `JobFailureContext` contains, similarly to the normal `JobContext`, a `UnitOfWork` instance that you can use to
perform transactional actions during your failure handler.

## Job queue handling

Each job type has a queue specifically for that job type. On startup, a queue processor is initialized for each known
job type. Each queue processor will process its queue sequentially, there is currently no concurrency for an individual
queue without starting multiple application instances.

## Stale job detection

What if your application crashes? How does the job queue ensure that any in-progress jobs are restarted?

When processing a job, the current processor is assigned as the job owner. An associated watchdog timeout value is that
continually updated for the duration of the processing. If the application crashes or hangs, other application instances
will eventually notice the expired timeout and forcibly reschedule the job. Any application instance afterwards may then
start processing the job like normal.

If the watchdog process is unable to refresh the timeout value, the job (or more specifically, the Kotlin coroutine for
the job) will be cancelled and not allowed to commit. This should only happen if the entire application hangs for a
prolonged period of time.

