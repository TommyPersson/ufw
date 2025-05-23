package io.tpersson.ufw.featuretoggles.internal

import io.tpersson.ufw.featuretoggles.FeatureToggleDefinition
import io.tpersson.ufw.keyvaluestore.KeyValueStore
import io.tpersson.ufw.test.TestClock
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock

internal class FeatureTogglesImplTest {

    private lateinit var keyValueStoreMock: KeyValueStore

    private lateinit var testClock: TestClock

    private lateinit var featureToggles: FeatureTogglesImpl

    @BeforeEach
    fun setUp() {
        keyValueStoreMock = mock<KeyValueStore>()
        testClock = TestClock()

        featureToggles = FeatureTogglesImpl(
            keyValueStore = keyValueStoreMock,
            clock = testClock
        )
    }

    @Test
    fun `Shall be able to get handle to feature toggle`() {
        val handle = featureToggles.get(TestToggles.TestFeatureToggle1)

        assertThat(handle.definition).isSameAs(TestToggles.TestFeatureToggle1)
    }

    @Test
    fun `Shall add used feature toggles to the list of knowns`() {
        featureToggles.get(TestToggles.TestFeatureToggle1)
        featureToggles.get(TestToggles.TestFeatureToggle2)

        assertThat(featureToggles.knownFeatureToggles["test-1"]).isSameAs(TestToggles.TestFeatureToggle1)
        assertThat(featureToggles.knownFeatureToggles["test-2"]).isSameAs(TestToggles.TestFeatureToggle2)
    }

    object TestToggles {
        object TestFeatureToggle1 : FeatureToggleDefinition {
            override val id = "test-1"
            override val title = "Test title 1"
            override val description = "Test description 1"
            override val default = true
        }
        object TestFeatureToggle2 : FeatureToggleDefinition {
            override val id = "test-2"
            override val title = "Test title 2"
            override val description = "Test description 2"
            override val default = false
        }
    }
}