package io.tpersson.ufw.core

import java.time.Clock
import java.time.InstantSource

public class CoreComponent private constructor(
    public val instantSource: InstantSource
) {
    public companion object {
        public fun create(
            instantSource: InstantSource = Clock.systemUTC()
        ): CoreComponent {
            return CoreComponent(instantSource)
        }
    }
}

@Suppress("UnusedReceiverParameter")
public val Components.Core: CoreComponent.Companion get() = CoreComponent