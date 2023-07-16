package io.tpersson.ufw.managed

import io.tpersson.ufw.core.logging.createLogger
import io.tpersson.ufw.core.utils.shortQualifiedName
import java.util.concurrent.atomic.AtomicBoolean

public abstract class Managed {

    private val logger = createLogger()

    private val _isRunning: AtomicBoolean = AtomicBoolean(false)
    public val isRunning: Boolean get() = _isRunning.get()

    private val name = this::class.shortQualifiedName

    public suspend fun start() {
        logger.info("Starting Managed instance: $name")

        try {
            onStarted()
        } catch (e: Exception) {
            logger.error("Error while starting Managed instance: $name", e)
            throw e
        }

        logger.info(" Started Managed instance: $name")
        _isRunning.set(true)
    }

    public suspend fun stop() {
        logger.info("Stopping Managed instance: $name")

        try {
            onStopped()
        } catch (e: Exception) {
            logger.error("Error while stopping Managed instance: $name", e)
            throw e
        }

        logger.info("Stopped Managed instance: $name")
        _isRunning.set(false)
    }

    protected abstract suspend fun onStarted()

    protected abstract suspend fun onStopped()

}