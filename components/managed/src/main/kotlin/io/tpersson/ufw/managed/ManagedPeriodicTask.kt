package io.tpersson.ufw.managed

import io.tpersson.ufw.core.utils.forever
import kotlinx.coroutines.delay
import java.time.Duration

// TODO add tests
public abstract class ManagedPeriodicTask(
    private val interval: Duration,
    private val errorDelay: Duration = Duration.ofSeconds(5),
    private val waitFirst: Boolean = true,
) : ManagedJob() {

    public abstract suspend fun runOnce()

    override suspend fun launch() {
        if (waitFirst) {
            delay(interval.toMillis())
        }

        forever(logger, errorDelay = errorDelay) {
            runOnce()
            delay(interval.toMillis())
        }
    }
}