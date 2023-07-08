package io.tpersson.ufw.jobqueue

import kotlin.reflect.KClass

public class JobQueueId<TJob : Job>(jobType: KClass<out TJob>)