package io.tpersson.ufw.jobqueue.v2

public interface DurableJobHandler<TJob : Any> {
    public suspend fun handle(item: TJob): Unit
}