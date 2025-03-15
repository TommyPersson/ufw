package io.tpersson.ufw.durablejobs

import org.intellij.lang.annotations.Language

public annotation class DurableJobTypeDefinition(
    val queueId: String = "",
    val type: String = "",
    @Language("Markdown")
    val description: String = "",
)