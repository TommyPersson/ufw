package io.tpersson.ufw.transactionalevents.handler

public enum class EventState(public val id: Int) {
    Scheduled(1),
    InProgress(2),
    Successful(3),
    Failed(4);

    public companion object {
        public fun fromId(id: Int): EventState {
            return entries.first { it.id == id }
        }
    }
}