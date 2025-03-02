package io.tpersson.ufw.managed

import io.tpersson.ufw.core.utils.shortQualifiedName
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.concurrent.atomic.AtomicBoolean

public abstract class Managed {

    protected val logger: Logger = LoggerFactory.getLogger(this::class.qualifiedName!!)

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
        if (!isRunning) {
            return
        }

        logger.info("Stopping Managed instance: $name")

        try {
            onStopped()
        } catch (e: Exception) {
            logger.error("Error while stopping Managed instance: $name", e)
            throw e
        }

        logger.info(" Stopped Managed instance: $name")
        _isRunning.set(false)
    }

    protected abstract suspend fun onStarted()

    protected abstract suspend fun onStopped()

}