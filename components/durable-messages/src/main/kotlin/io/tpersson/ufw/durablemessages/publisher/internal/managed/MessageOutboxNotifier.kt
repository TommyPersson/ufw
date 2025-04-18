package io.tpersson.ufw.durablemessages.publisher.internal.managed

import io.tpersson.ufw.core.concurrency.ConsumerSignal
import jakarta.inject.Inject
import jakarta.inject.Singleton

@Singleton
public class MessageOutboxNotifier @Inject constructor() {
    public val signal: ConsumerSignal = ConsumerSignal()
}

