package io.tpersson.ufw.featuretoggles

import org.intellij.lang.annotations.Language

public interface FeatureToggleDefinition {
    public val id: String
    public val title: String

    @get:Language("Markdown")
    public val description: String
    public val default: Boolean
}