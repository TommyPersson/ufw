package io.tpersson.ufw.transactionalevents.handler

import java.time.Instant

public sealed class FailureAction {
    public class Reschedule(public val at: Instant) : FailureAction()
    public object RescheduleNow : FailureAction()
    public object GiveUp : FailureAction()
}