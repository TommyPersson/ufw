package io.tpersson.ufw.databasequeue.worker

public class UnableToTransformWorkItemException(
    message: String,
    cause: Throwable
) : Exception(message, cause)