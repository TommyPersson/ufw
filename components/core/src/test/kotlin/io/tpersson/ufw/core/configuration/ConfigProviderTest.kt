@file:Suppress("ClassName", "RemoveRedundantBackticks")

package io.tpersson.ufw.core.configuration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import java.io.File
import java.time.Duration

internal class ConfigProviderTest {

    @BeforeEach
    fun setUp() {
        System.clearProperty("base.test.duration")
    }

    @TestFactory
    fun `Nullability & Defaults`() = ConfigProvider.TextFormat.entries.map { textFormat ->
        DynamicTest.dynamicTest(textFormat.name, {
            val configProvider = getTestConfigProvider(textFormat)

            assertThat(configProvider.get(TestConfig.Nullable)).isNull()
            assertThat(configProvider.get(TestConfig.Missing)).isNull()
            assertThat(configProvider.get(TestConfig.NullableWithDefault)).isEqualTo("default")
            assertThat(configProvider.get(TestConfig.MissingWithDefault)).isEqualTo("default")
        })
    }

    @TestFactory
    fun `Java-Time Types`() = ConfigProvider.TextFormat.entries.map { textFormat ->
        DynamicTest.dynamicTest(textFormat.name, {
            val configProvider = getTestConfigProvider(textFormat)

            assertThat(configProvider.get(TestConfig.Duration)).isEqualTo(Duration.ofSeconds(10))
            assertThat(configProvider.get(TestConfig.NullableDuration)).isEqualTo(null)
            assertThat(configProvider.get(TestConfig.MissingDuration)).isEqualTo(null)
            assertThat(configProvider.get(TestConfig.MissingDurationWithDefault)).isEqualTo(Duration.ofSeconds(5))
        })
    }

    @TestFactory
    fun `Loading from file`() = ConfigProvider.TextFormat.entries.map { textFormat ->
        DynamicTest.dynamicTest(textFormat.name) {
            val configProvider = ConfigProvider.fromFile(
                path = getTestFilePath(textFormat),
                format = textFormat,
                basePath = "base",
                env = env
            )!!

            assertThat(configProvider.get(TestConfig.Duration)).isEqualTo(Duration.ofSeconds(10))
        }
    }

    @TestFactory
    fun `Loading from text`() = ConfigProvider.TextFormat.entries.map { textFormat ->
        DynamicTest.dynamicTest(textFormat.name) {
            val configProvider = ConfigProvider.fromText(
                text = File(getTestFilePath(textFormat)).readText(),
                format = textFormat,
                basePath = "base",
                env = env
            )

            assertThat(configProvider.get(TestConfig.Duration)).isEqualTo(Duration.ofSeconds(10))
        }
    }

    @TestFactory
    fun `Loading from resource`() = ConfigProvider.TextFormat.entries.map { textFormat ->
        DynamicTest.dynamicTest(textFormat.name) {
            val configProvider = ConfigProvider.fromResource(
                resourcePath = "/" + getTestFileName(textFormat),
                resourceOwner = this::class.java,
                format = textFormat,
                basePath = "base",
                env = env
            )!!

            assertThat(configProvider.get(TestConfig.Duration)).isEqualTo(Duration.ofSeconds(10))
        }
    }

    @TestFactory
    fun `Overrides from system properties`() = ConfigProvider.TextFormat.entries.map { textFormat ->
        DynamicTest.dynamicTest(textFormat.name) {
            System.setProperty("base.test.duration", "PT15S")

            val configProvider = ConfigProvider.fromResource(
                resourcePath = "/" + getTestFileName(textFormat),
                resourceOwner = this::class.java,
                format = textFormat,
                basePath = "base",
                env = env
            )!!

            assertThat(configProvider.get(TestConfig.Duration)).isEqualTo(Duration.ofSeconds(15))
        }
    }

    @TestFactory
    fun `Overrides from environment variables`() = ConfigProvider.TextFormat.entries.map { textFormat ->
        DynamicTest.dynamicTest(textFormat.name) {
            val configProvider = ConfigProvider.fromResource(
                resourcePath = "/" + getTestFileName(textFormat),
                resourceOwner = this::class.java,
                format = textFormat,
                basePath = "base",
                env = env
            )!!

            assertThat(configProvider.get(TestConfig.DurationFromEnv)).isEqualTo(Duration.ofSeconds(20))
        }
    }

    @Test
    fun `Empty provider returns only defaults`() {
        val configProvider = ConfigProvider.empty()

        assertThat(configProvider.get(TestConfig.Missing)).isEqualTo(null)
        assertThat(configProvider.get(TestConfig.NullableWithDefault)).isEqualTo("default")
        assertThat(configProvider.get(TestConfig.DurationFromEnv)).isEqualTo(Duration.ofSeconds(5))
    }

    companion object {
        private val env = mapOf("DURATION_FROM_ENV" to "PT20S")

        fun getTestConfigProvider(textFormat: ConfigProvider.TextFormat): ConfigProvider {
            val file = File(getTestFilePath(textFormat))
            val text = file.readText()

            return ConfigProvider.fromText(
                text = text,
                format = textFormat,
                basePath = "base",
                env = env
            )
        }

        fun getTestFilePath(textFormat: ConfigProvider.TextFormat): String {
            return "src/test/resources/${getTestFileName(textFormat)}"
        }

        fun getTestFileName(textFormat: ConfigProvider.TextFormat): String {
            return when (textFormat) {
                ConfigProvider.TextFormat.JSON -> "configuration/test-config.json"
                ConfigProvider.TextFormat.TOML -> "configuration/test-config.toml"
                ConfigProvider.TextFormat.YAML -> "configuration/test-config.yaml"
                ConfigProvider.TextFormat.JavaProperties -> "configuration/test-config.properties"
            }
        }
    }

    object TestConfig {
        val Nullable: ConfigElement<String?> =
            ConfigElement.of<String?>("test", "nullable", default = null)

        val Missing: ConfigElement<String?> =
            ConfigElement.of<String?>("test", "missing", default = null)

        val NullableWithDefault: ConfigElement<String?> =
            ConfigElement.of<String?>("test", "nullableWithDefault", default = "default")

        val MissingWithDefault: ConfigElement<String?> =
            ConfigElement.of<String?>("test", "missingWithDefault", default = "default")

        val Duration: ConfigElement<Duration> =
            ConfigElement.of<Duration>("test", "duration", default = java.time.Duration.ofSeconds(5))

        val NullableDuration: ConfigElement<Duration?> =
            ConfigElement.of<Duration?>("test", "nullableDuration", default = null)

        val MissingDuration: ConfigElement<Duration?> =
            ConfigElement.of<Duration?>("test", "missingDuration", default = null)

        val MissingDurationWithDefault: ConfigElement<Duration?> =
            ConfigElement.of<Duration?>("test", "missingDurationWithDefault", default = java.time.Duration.ofSeconds(5))

        val DurationFromEnv: ConfigElement<Duration> =
            ConfigElement.of<Duration>("test", "durationFromEnv", default = java.time.Duration.ofSeconds(5))
    }
}