package io.tpersson.ufw.featuretoggles.internal

import io.tpersson.ufw.featuretoggles.FeatureToggleDefinition
import io.tpersson.ufw.featuretoggles.FeatureToggleHandle
import io.tpersson.ufw.keyvaluestore.KeyValueStore
import jakarta.inject.Inject
import jakarta.inject.Singleton
import java.time.Clock
import java.util.concurrent.ConcurrentHashMap

@Singleton
public class FeatureTogglesImpl @Inject constructor(
    private val keyValueStore: KeyValueStore,
    private val clock: Clock,
) : FeatureTogglesInternal {

    private val _knownFeatureToggles: MutableMap<String, FeatureToggleDefinition> = ConcurrentHashMap()

    override val knownFeatureToggles: Map<String, FeatureToggleDefinition>
        get() = _knownFeatureToggles

    public override fun get(definition: FeatureToggleDefinition): FeatureToggleHandle {
        if (!_knownFeatureToggles.containsKey(definition.id)) {
            _knownFeatureToggles[definition.id] = definition
        }

        return FeatureToggleHandleImpl(definition, keyValueStore, clock)
    }
}

