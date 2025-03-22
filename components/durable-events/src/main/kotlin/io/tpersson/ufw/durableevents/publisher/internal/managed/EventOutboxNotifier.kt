package io.tpersson.ufw.durableevents.publisher.internal.managed

import io.tpersson.ufw.core.concurrency.ConsumerSignal
import jakarta.inject.Inject
import jakarta.inject.Singleton

@Singleton
public class EventOutboxNotifier @Inject constructor() {
    public val signal: ConsumerSignal = ConsumerSignal()
}

