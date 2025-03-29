package io.tpersson.ufw.mediator.annotations

import org.intellij.lang.annotations.Language

public annotation class AdminRequest(
    val name: String,
    @Language("Markdown")
    val description: String,
)
