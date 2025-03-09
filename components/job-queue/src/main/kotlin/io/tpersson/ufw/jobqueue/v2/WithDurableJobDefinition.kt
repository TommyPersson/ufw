package io.tpersson.ufw.jobqueue.v2

import org.intellij.lang.annotations.Language

// TODO name?
public annotation class WithDurableJobDefinition(
    val queueId: String = "",
    val type: String = "",
    @Language("Markdown")
    val description: String = "",
)