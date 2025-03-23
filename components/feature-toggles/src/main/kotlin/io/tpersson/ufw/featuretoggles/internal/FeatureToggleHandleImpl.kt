package io.tpersson.ufw.featuretoggles.internal

import io.tpersson.ufw.featuretoggles.FeatureToggleDefinition
import io.tpersson.ufw.featuretoggles.FeatureToggleHandle
import io.tpersson.ufw.keyvaluestore.KeyValueStore
import java.time.InstantSource

public class FeatureToggleHandleImpl(
    public override val definition: FeatureToggleDefinition,
    private val keyValueStore: KeyValueStore,
    private val clock: InstantSource,
) : FeatureToggleHandle {

    private val key = KeyValueStore.Key.of<FeatureToggleData>("${Constants.KEY_PREFIX}${definition.id}")

    override suspend fun isEnabled(): Boolean {
        val data = keyValueStore.get(key)
        if (data == null) {
            val default = definition.default
            val defaultData = FeatureToggleData(definition, default, clock.instant())
            keyValueStore.put(key, defaultData)
            return defaultData.isEnabled
        }

        if (data.value.definition != definition) {
            keyValueStore.put(key, data.value.copy(definition = definition))
        }

        return data.value.isEnabled
    }

    override suspend fun enable() {
        val data = keyValueStore.get(key)
        if (data?.value?.isEnabled == true) {
            return
        }

        keyValueStore.put(key, FeatureToggleData(definition, true, clock.instant()))
    }

    override suspend fun disable() {
        val data = keyValueStore.get(key)
        if (data?.value?.isEnabled == false) {
            return
        }

        keyValueStore.put(key, FeatureToggleData(definition, false, clock.instant()))
    }
}
