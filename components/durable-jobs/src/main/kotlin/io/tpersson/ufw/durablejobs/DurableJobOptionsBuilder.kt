package io.tpersson.ufw.durablejobs

import java.time.Instant

public data class DurableJobOptionsBuilder(
    var scheduleFor: Instant? = null
)