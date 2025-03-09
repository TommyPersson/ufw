package io.tpersson.ufw.jobqueue

import org.intellij.lang.annotations.Language

// TODO name?
public annotation class WithDurableJobDefinition(
    val queueId: String = "",
    val type: String = "",
    @Language("Markdown")
    val description: String = "",
)