package io.tpersson.ufw.featuretoggles

import org.intellij.lang.annotations.Language

public data class FeatureToggleDefinition(
    public val id: String,
    public val title: String,
    @Language("Markdown")
    public val description: String,
    public val default: Boolean,
)