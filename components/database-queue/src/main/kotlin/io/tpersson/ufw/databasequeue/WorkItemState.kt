package io.tpersson.ufw.databasequeue

public object WorkItemState {
    public const val SCHEDULED: Int = 1
    public const val IN_PROGRESS: Int = 2
    public const val SUCCESSFUL: Int = 3
    public const val FAILED: Int = 4
    public const val PAUSED: Int = 5
    public const val CANCELLED: Int = 6
}