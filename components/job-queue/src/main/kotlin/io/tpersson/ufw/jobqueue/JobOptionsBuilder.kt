package io.tpersson.ufw.jobqueue

import java.time.Instant

public data class JobOptionsBuilder(
    var scheduleFor: Instant? = null
)