package io.tpersson.ufw.core

import io.opentelemetry.api.OpenTelemetry
import io.opentelemetry.api.metrics.Meter
import jakarta.inject.Inject
import jakarta.inject.Provider
import java.util.*

public class UFWMeterProvider @Inject constructor(
    private val openTelemetry: Optional<OpenTelemetry>
) : Provider<Optional<Meter>> {

    private val builder = openTelemetry.map { it.meterBuilder("ufw").build() }

    override fun get(): Optional<Meter> {
        return builder
    }
}