package io.tpersson.ufw.core.configuration

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.javaprop.JavaPropsFactory
import com.fasterxml.jackson.dataformat.toml.TomlFactory
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import org.slf4j.LoggerFactory
import java.io.File
import java.time.Duration

// TODO validation. How, with dynamic registration?
public interface ConfigProvider {
    public fun <T : Any?> get(element: ConfigElement<T>): T

    public companion object {

        private val logger = LoggerFactory.getLogger(ConfigProvider::class.java)

        public fun fromText(
            text: String,
            format: TextFormat,
            basePath: String? = "ufw",
            env: Map<String, String> = System.getenv(),
        ): ConfigProvider {
            val mapper = when (format) {
                TextFormat.JSON -> jsonMapper
                TextFormat.TOML -> tomlMapper
                TextFormat.YAML -> yamlMapper
                TextFormat.JavaProperties -> javaPropertiesMapper
            }

            return JacksonConfigProvider(mapper, text, basePath, env)
        }

        public fun fromFile(
            path: String,
            format: TextFormat,
            basePath: String? = "ufw",
            env: Map<String, String> = System.getenv(),
        ): ConfigProvider? {
            val file = File(path)
            if (!file.exists()) {
                logger.info("Searching for config in file '$path': Not found!")
                return null
            }

            logger.info("Searching for config in file '$path': Found!")

            val text = file.readText()
            return fromText(text, format, basePath, env)
        }

        public fun fromResource(
            resourcePath: String,
            resourceOwner: Class<*>,
            format: TextFormat,
            basePath: String? = "ufw",
            env: Map<String, String> = System.getenv(),
        ): ConfigProvider? {
            val text = resourceOwner.getResourceAsStream(resourcePath)
                ?.readAllBytes()
                ?.decodeToString()

            if (text == null) {
                logger.info("Searching for config in resource '$resourcePath': Not found!")
                return null
            }

            logger.info("Searching for config in resource '$resourcePath': Found!")

            return fromText(text, format, basePath, env)
        }

        public fun default(): ConfigProvider {
            return fromFile("ufw.json", format = TextFormat.JSON)
                ?: fromFile("ufw.yaml", format = TextFormat.YAML)
                ?: fromFile("ufw.toml", format = TextFormat.TOML)
                ?: fromFile("ufw.properties", format = TextFormat.JavaProperties)
                ?: fromResource("/ufw.json", format = TextFormat.JSON, resourceOwner = this::class.java)
                ?: fromResource("/ufw.yaml", format = TextFormat.YAML, resourceOwner = this::class.java)
                ?: fromResource("/ufw.toml", format = TextFormat.TOML, resourceOwner = this::class.java)
                ?: fromResource("/ufw.properties", format = TextFormat.JavaProperties, resourceOwner = this::class.java)
                ?: empty()
        }

        public fun empty(): ConfigProvider {
            return EmptyConfigProvider()
        }

        private val jsonMapper = ObjectMapper().findAndRegisterModules()
        private val yamlMapper = ObjectMapper(YAMLFactory()).findAndRegisterModules()
        private val tomlMapper = ObjectMapper(TomlFactory()).findAndRegisterModules()
        private val javaPropertiesMapper = ObjectMapper(JavaPropsFactory()).findAndRegisterModules()
    }

    public enum class TextFormat {
        JSON,
        TOML,
        YAML,
        JavaProperties,
    }
}

public val Configs.DatabaseQueue: DatabaseQueueConfigs get() = DatabaseQueueConfigs

public object DatabaseQueueConfigs {
    public val FallbackPollingInterval: ConfigElement<Duration> = ConfigElement.of<Duration>(
        "database-queue",
        "fallback-polling-interval",
        default = Duration.ofSeconds(10)
    )
    public val Thing: ConfigElement<Duration?> = ConfigElement.of<Duration?>(
        "database-queue",
        "thing",
        default = null
    )
    public val Stuff: ConfigElement<Int?> = ConfigElement.of<Int?>(
        "database-queue",
        "stuff",
        default = 123
    )
}


