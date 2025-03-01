package io.tpersson.ufw.databasequeue

public enum class WorkItemState(
    public val dbOrdinal: Int,
) {
    SCHEDULED(1),
    IN_PROGRESS(2),
    SUCCESSFUL(3),
    FAILED(4),
    PAUSED(5),
    CANCELLED(6);

    public companion object {
        public fun fromDbOrdinal(dbOrdinal: Int): WorkItemState {
            return when (dbOrdinal) {
                1 -> SCHEDULED
                2 -> IN_PROGRESS
                3 -> SUCCESSFUL
                4 -> FAILED
                5 -> PAUSED
                6 -> CANCELLED
                else -> error("Unknown WorkItemState: $dbOrdinal")
            }
        }
    }
}