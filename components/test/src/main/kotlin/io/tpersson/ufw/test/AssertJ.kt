package io.tpersson.ufw.test

import org.assertj.core.api.AbstractInstantAssert
import java.time.Instant

public fun <SELF : AbstractInstantAssert<SELF>> AbstractInstantAssert<SELF>.isEqualToIgnoringNanos(expected: Instant): SELF {
    return this.usingEquals({ a, b -> a.toEpochMilli() == b.toEpochMilli() }).isEqualTo(expected)
}