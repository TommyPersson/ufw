package io.tpersson.ufw.featuretoggles.internal

import io.tpersson.ufw.featuretoggles.FeatureToggleHandle
import io.tpersson.ufw.keyvaluestore.KeyValueStore
import java.time.InstantSource

public class FeatureToggleHandleImpl(
    public override val featureToggleId: String,
    private val keyValueStore: KeyValueStore,
    private val clock: InstantSource,
) : FeatureToggleHandle {

    private val key = KeyValueStore.Key.of<FeatureToggleData>("__ft__$featureToggleId")

    override suspend fun isEnabled(default: Boolean): Boolean {
        val data = keyValueStore.get(key)
        if (data == null) {
            val defaultData = FeatureToggleData(default, clock.instant())
            keyValueStore.put(key, defaultData)
            return defaultData.isEnabled
        }

        return data.value.isEnabled
    }

    override suspend fun enable() {
        val data = keyValueStore.get(key)
        if (data?.value?.isEnabled == true) {
            return
        }

        keyValueStore.put(key, FeatureToggleData(true, clock.instant()))
    }

    override suspend fun disable() {
        val data = keyValueStore.get(key)
        if (data?.value?.isEnabled == false) {
            return
        }

        keyValueStore.put(key, FeatureToggleData(false, clock.instant()))
    }
}
