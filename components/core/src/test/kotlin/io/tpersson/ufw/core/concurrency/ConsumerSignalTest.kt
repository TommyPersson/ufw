package io.tpersson.ufw.core.concurrency;

import kotlinx.coroutines.*
import kotlinx.coroutines.time.withTimeout
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.Duration
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

internal class ConsumerSignalTest {

    @Test
    fun `Allow any existing waiter to continue`(): Unit = runBlocking {
        val signal = ConsumerSignal()

        val jobFinished = AtomicBoolean(false)

        val waitingJob = launchWaiter {
            if (signal.wait(100.ms)) {
                jobFinished.set(true)
            }
        }

        delay(10)

        assertThat(jobFinished).isFalse

        signal.signal()

        waitingJob.join()

        assertThat(jobFinished).isTrue
    }

    @Test
    fun `Keep state so that next 'wait' can continue immediately`(): Unit = runBlocking {
        val signal = ConsumerSignal()

        val jobFinished = AtomicBoolean(false)

        signal.signal()

        val waitingJob = launchWaiter {
            if (signal.wait(100.ms)) {
                jobFinished.set(true)
            }
        }

        waitingJob.join()

        assertThat(jobFinished).isTrue
    }

    @Test
    fun `Do not keep state longer than a single signal`(): Unit = runBlocking {
        val signal = ConsumerSignal()

        val numWaitsCompleted = AtomicInteger(0)

        signal.signal()

        val waitingJob = launchWaiter {
            if (signal.wait(100.ms)) {
                numWaitsCompleted.incrementAndGet()
            }

            if (signal.wait(100.ms)) {
                numWaitsCompleted.incrementAndGet()
            }
        }

        waitingJob.join()

        assertThat(numWaitsCompleted.get()).isEqualTo(1)
    }

    @Test
    fun `Can use signal multiple times`(): Unit = runBlocking {
        val signal = ConsumerSignal()
        val returnSignal = ConsumerSignal()

        val numWaitsCompleted = AtomicInteger(0)

        signal.signal()

        val waitingJob = launchWaiter {
            if (signal.wait(100.ms)) {
                numWaitsCompleted.incrementAndGet()
            }

            returnSignal.signal()

            if (signal.wait(100.ms)) {
                numWaitsCompleted.incrementAndGet()
            }
        }

        returnSignal.wait(100.ms)

        signal.signal()

        waitingJob.join()

        assertThat(numWaitsCompleted.get()).isEqualTo(2)
    }

    private fun CoroutineScope.launchWaiter(block: suspend () -> Unit): Job {
        return launch {
            withTimeout(Duration.ofSeconds(10)) {
                block()
            }
        }
    }

    private val Int.ms: Duration get() = Duration.ofMillis(this.toLong())
}