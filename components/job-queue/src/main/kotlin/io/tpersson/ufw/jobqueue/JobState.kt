package io.tpersson.ufw.jobqueue

public enum class JobState(public val id: Int) {
    Scheduled(1),
    InProgress(2),
    Successful(3),
    Failed(4),
    Cancelled(5);

    public companion object {
        @OptIn(ExperimentalStdlibApi::class)
        public fun fromId(id: Int): JobState {
            return entries.first { it.id == id }
        }
    }
}