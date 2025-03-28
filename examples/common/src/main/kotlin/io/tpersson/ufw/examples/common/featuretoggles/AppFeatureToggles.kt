package io.tpersson.ufw.examples.common.featuretoggles

import io.tpersson.ufw.featuretoggles.FeatureToggleDefinition

internal object AppFeatureToggles {
    object PeriodicJobScheduler: FeatureToggleDefinition {
        override val id = "PeriodicJobScheduler"
        override val title = "Periodic Job Scheduler"
        override val description = """
Will enable the periodic job scheduler.

What happens if we use more *advanced formatting*? Will it wrap correctly? Or display weirdly? We don't know yet. 
Looks like we need to make it **even longer**?
""".trimIndent()
        override val default = true
    }

    object PeriodicEventPublisher: FeatureToggleDefinition {
        override val id = "PeriodicEventPublisher"
        override val title = "Periodic Event Publisher"
        override val description = "Will enable the periodic event publisher."
        override val default = true
    }

    object PeriodicLogger: FeatureToggleDefinition {
        override val id = "PeriodicLogger"
        override val title = "Periodic Logger"
        override val description = "Logs a heartbeat message in a short interval."
        override val default = true
    }
}