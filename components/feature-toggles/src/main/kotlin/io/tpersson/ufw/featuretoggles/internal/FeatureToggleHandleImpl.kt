package io.tpersson.ufw.featuretoggles.internal

import io.tpersson.ufw.featuretoggles.FeatureToggle
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

    override suspend fun get(): FeatureToggle {
        val data = keyValueStore.get(key)
        if (data == null) {
            val now = clock.instant()
            val defaultData = FeatureToggleData(
                definition = definition,
                isEnabled = definition.default,
                stateChangedAt = now
            )

            keyValueStore.put(key, defaultData)

            return FeatureToggle(
                id = definition.id,
                title = definition.title,
                description = definition.description,
                stateChangedAt = now,
                createdAt = now,
                isEnabled = defaultData.isEnabled,
            )
        }

        if (data.value.definition != definition) {
            keyValueStore.put(key, data.value.copy(definition = definition))
        }

        return FeatureToggle(
            id = definition.id,
            title = definition.title,
            description = definition.description,
            stateChangedAt = data.value.stateChangedAt,
            createdAt = data.createdAt,
            isEnabled = data.value.isEnabled,
        )
    }

    override suspend fun isEnabled(): Boolean {
        return get().isEnabled
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
