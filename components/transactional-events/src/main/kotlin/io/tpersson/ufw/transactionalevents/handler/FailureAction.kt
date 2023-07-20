package io.tpersson.ufw.transactionalevents.handler

import java.time.Instant

public sealed class FailureAction {
    public data class Reschedule(public val at: Instant) : FailureAction()
    public data object RescheduleNow : FailureAction()
    public data object GiveUp : FailureAction()
}