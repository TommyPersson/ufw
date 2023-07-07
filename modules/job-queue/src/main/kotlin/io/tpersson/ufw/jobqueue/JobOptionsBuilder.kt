package io.tpersson.ufw.jobqueue

import java.time.Duration
import java.time.Instant

public data class JobOptionsBuilder(
    var scheduleFor: Instant? = null,
    var timeoutAfter: Duration? = null,
    var retainOnFailure: Duration? = null,
    var retainOnSuccess: Duration? = null,
)