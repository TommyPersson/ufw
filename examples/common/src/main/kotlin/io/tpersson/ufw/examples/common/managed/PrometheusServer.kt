package io.tpersson.ufw.examples.common.managed

import com.sun.net.httpserver.HttpServer
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import io.tpersson.ufw.managed.Managed
import jakarta.inject.Inject
import java.net.InetSocketAddress
import kotlin.concurrent.thread

public class PrometheusServer @Inject constructor(
    private val meterRegistry: MeterRegistry,
) : Managed() {

    private var server: HttpServer? = null

    override suspend fun onStarted() {
        if (meterRegistry !is PrometheusMeterRegistry) {
            logger.info("Not a PrometheusMeterRegistry, ignoring.")
            return
        }

        server = HttpServer.create(InetSocketAddress(8082), 0).also {
            it.createContext("/prometheus") { exchange ->
                val response = meterRegistry.scrape().toByteArray(Charsets.UTF_8)
                exchange.sendResponseHeaders(200, response.size.toLong())
                exchange.responseBody.use { os ->
                    os.write(response)
                }
            }

            thread(isDaemon = true) {
                it.start()
            }
        }
    }

    override suspend fun onStopped() {
        server?.stop(0)
    }
}