package io.tpersson.ufw.featuretoggles.internal

import io.tpersson.ufw.featuretoggles.FeatureToggleHandle
import io.tpersson.ufw.featuretoggles.FeatureToggles
import io.tpersson.ufw.keyvaluestore.KeyValueStore
import jakarta.inject.Inject
import java.time.InstantSource

public class FeatureTogglesImpl @Inject constructor(
    private val keyValueStore: KeyValueStore,
    private val clock: InstantSource,
) : FeatureToggles {
    public override fun get(id: String): FeatureToggleHandle {
        return FeatureToggleHandleImpl(id, keyValueStore, clock)
    }
}

