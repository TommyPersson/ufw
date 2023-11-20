package io.tpersson.ufw.jobqueue

import java.time.Instant

public sealed class FailureAction {
    public class Reschedule(public val at: Instant) : FailureAction()
    public data object RescheduleNow : FailureAction()
    public data object GiveUp : FailureAction()
}