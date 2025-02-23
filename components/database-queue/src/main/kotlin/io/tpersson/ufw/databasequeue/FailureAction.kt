package io.tpersson.ufw.databasequeue

import java.time.Instant

public sealed class FailureAction {
    public data object GiveUp : FailureAction()
    public data object RescheduleNow : FailureAction()
    public data class RescheduleAt(val at: Instant) : FailureAction()
}