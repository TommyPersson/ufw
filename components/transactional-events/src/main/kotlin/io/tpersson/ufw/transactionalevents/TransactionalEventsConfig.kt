package io.tpersson.ufw.transactionalevents

public data class TransactionalEventsConfig(
    val thing: Boolean = true
) {
    public companion object {
        public val default: TransactionalEventsConfig = TransactionalEventsConfig()
    }
}