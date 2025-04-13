package io.tpersson.ufw.core.configuration

import com.fasterxml.jackson.core.JsonPointer
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import kotlin.reflect.jvm.jvmName

public class JacksonConfigProvider(
    private val objectMapper: ObjectMapper,
    private val text: String,
    private val basePath: String?,
    private val env: Map<String, String>,
) : ConfigProvider {

    private var envVarRegex = """\{\{([A-Za-z0-9_]*)}}""".toRegex()

    private val tree: JsonNode = run {
        var replacedText = text

        val referencedVars = envVarRegex.findAll(text).map { it.groupValues[1] }
        for (referencedVar in referencedVars) {
            val replacement = env[referencedVar]
                ?: error("Environmental variable not found: '$referencedVar'")

            replacedText = replacedText.replace("{{$referencedVar}}", replacement)
        }

        objectMapper.readTree(replacedText)
    }

    override fun <T : Any?> get(element: ConfigElement<T>): T {
        // TODO cache?
        val fullPath = (listOf(basePath) + element.path).filterNotNull()
        val dottedPath = fullPath.joinToString(separator = ".")

        val systemValue = System.getProperty(dottedPath)
        if (systemValue != null) {
            return objectMapper.convertValue(systemValue, element.type.java) as T
        }

        val pointer = JsonPointer.valueOf("/" + fullPath.joinToString("/"))
        val node = tree.at(pointer)

        if (node.isMissingNode) {
            return element.default
        }

        return objectMapper.convertValue(node, element.type.java)
    }

    override fun toString(): String {
        return "JacksonConfigurationProvider[factory=${objectMapper.factory::class.jvmName}]"
    }
}
