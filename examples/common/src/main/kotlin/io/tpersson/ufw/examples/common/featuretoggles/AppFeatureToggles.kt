package io.tpersson.ufw.examples.common.featuretoggles

import io.tpersson.ufw.featuretoggles.FeatureToggleDefinition

public object AppFeatureToggles {
    public val PeriodicJobScheduler: FeatureToggleDefinition = FeatureToggleDefinition(
        id = "PeriodicJobScheduler",
        title = "Periodic Job Scheduler",
        description = """
Will enable the periodic job scheduler.

What happens if we use more *advanced formatting*? Will it wrap correctly? Or display weirdly? We don't know yet. 
Looks like we need to make it **even longer**?
""".trimIndent(),
        default = true,
    )

    public val PeriodicEventPublisher: FeatureToggleDefinition = FeatureToggleDefinition(
        id = "PeriodicEventPublisher",
        title = "Periodic Event Publisher",
        description = "Will enable the periodic event publisher.",
        default = true,
    )
}