package io.tpersson.ufw.featuretoggles.internal

import io.tpersson.ufw.core.component.CoreComponent
import io.tpersson.ufw.featuretoggles.FeatureToggleDefinition
import io.tpersson.ufw.keyvaluestore.KeyValueStore
import io.tpersson.ufw.keyvaluestore.KeyValueStoreImpl
import io.tpersson.ufw.keyvaluestore.storageengine.InMemoryStorageEngine
import io.tpersson.ufw.test.TestClock
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Duration

internal class FeatureToggleHandleImplTest {

    private lateinit var clock: TestClock

    private lateinit var keyValueStore: KeyValueStore

    @BeforeEach
    fun setUp() {
        clock = TestClock()

        keyValueStore = KeyValueStoreImpl(
            storageEngine = InMemoryStorageEngine(),
            clock = clock,
            objectMapper = CoreComponent.defaultObjectMapper
        )
    }

    @Test
    fun `isEnabled - Shall return default value for first time`(): Unit = runBlocking {
        val handle1 = handleOf(TestToggles.DefaultEnabledFeatureToggle)

        assertThat(handle1.isEnabled()).isEqualTo(TestToggles.DefaultEnabledFeatureToggle.default)

        val handle2 = handleOf(TestToggles.DefaultDisabledFeatureToggle)

        assertThat(handle2.isEnabled()).isEqualTo(TestToggles.DefaultDisabledFeatureToggle.default)
    }

    @Test
    fun `get - Shall return defaults for first time`(): Unit = runBlocking {
        val now = clock.instant()

        val handle1 = handleOf(TestToggles.DefaultEnabledFeatureToggle)
        val toggle1 = handle1.get()

        assertThat(toggle1.id).isEqualTo(TestToggles.DefaultEnabledFeatureToggle.id)
        assertThat(toggle1.title).isEqualTo(TestToggles.DefaultEnabledFeatureToggle.title)
        assertThat(toggle1.description).isEqualTo(TestToggles.DefaultEnabledFeatureToggle.description)
        assertThat(toggle1.stateChangedAt).isEqualTo(now)
        assertThat(toggle1.createdAt).isEqualTo(now)
        assertThat(toggle1.isEnabled).isEqualTo(TestToggles.DefaultEnabledFeatureToggle.default)

        val handle2 = handleOf(TestToggles.DefaultDisabledFeatureToggle)
        val toggle2 = handle2.get()

        assertThat(toggle2.id).isEqualTo(TestToggles.DefaultDisabledFeatureToggle.id)
        assertThat(toggle2.title).isEqualTo(TestToggles.DefaultDisabledFeatureToggle.title)
        assertThat(toggle2.description).isEqualTo(TestToggles.DefaultDisabledFeatureToggle.description)
        assertThat(toggle2.stateChangedAt).isEqualTo(now)
        assertThat(toggle2.createdAt).isEqualTo(now)
        assertThat(toggle2.isEnabled).isEqualTo(TestToggles.DefaultDisabledFeatureToggle.default)
    }

    @Test
    fun `enable - Shall enable the toggle`(): Unit = runBlocking {
        val handle = handleOf(TestToggles.DefaultDisabledFeatureToggle)

        handle.enable()

        assertThat(handle.isEnabled()).isTrue()
    }

    @Test
    fun `enable - Shall update state changed timestamp`(): Unit = runBlocking {
        val oldNow = clock.instant()

        val handle = handleOf(TestToggles.DefaultDisabledFeatureToggle)
        handle.get() // Cause initial creation

        clock.advance(Duration.ofHours(1))

        handle.enable()

        assertThat(handle.get().createdAt).isEqualTo(oldNow)
        assertThat(handle.get().stateChangedAt).isEqualTo(clock.instant())
    }

    @Test
    fun `disable - Shall disable the toggle`(): Unit = runBlocking {
        val handle = handleOf(TestToggles.DefaultEnabledFeatureToggle)

        handle.disable()

        assertThat(handle.isEnabled()).isFalse()
    }

    @Test
    fun `disable - Shall update state changed timestamp`(): Unit = runBlocking {
        val oldNow = clock.instant()

        val handle = handleOf(TestToggles.DefaultEnabledFeatureToggle)
        handle.get() // Cause initial creation

        clock.advance(Duration.ofHours(1))

        handle.disable()

        assertThat(handle.get().createdAt).isEqualTo(oldNow)
        assertThat(handle.get().stateChangedAt).isEqualTo(clock.instant())
    }

    private fun handleOf(definition: FeatureToggleDefinition): FeatureToggleHandleImpl {
        return FeatureToggleHandleImpl(
            definition = definition,
            keyValueStore = keyValueStore,
            clock = clock
        )
    }

    private object TestToggles {
        object DefaultEnabledFeatureToggle : FeatureToggleDefinition {
            override val id = "test-1"
            override val title = "Test title 1"
            override val description = "Test description 1"
            override val default = true
        }
        object DefaultDisabledFeatureToggle : FeatureToggleDefinition {
            override val id = "test-2"
            override val title = "Test title 2"
            override val description = "Test description 2"
            override val default = false
        }
    }
}