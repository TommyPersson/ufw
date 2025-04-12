package io.tpersson.ufw.mediator.guice

import io.tpersson.ufw.mediator.Mediator
import io.tpersson.ufw.mediator.internal.MediatorInternal
import jakarta.inject.Inject
import jakarta.inject.Provider
import jakarta.inject.Singleton

@Singleton
public class MediatorProvider @Inject constructor(
    private val mediator: MediatorInternal
) : Provider<Mediator> {

    override fun get(): Mediator {
        return mediator
    }
}

