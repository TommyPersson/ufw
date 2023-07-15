package io.tpersson.ufw.examples.common

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.opentelemetry.api.OpenTelemetry
import io.opentelemetry.api.common.Attributes
import io.opentelemetry.exporter.logging.LoggingMetricExporter
import io.opentelemetry.exporter.prometheus.PrometheusHttpServer
import io.opentelemetry.exporter.prometheus.PrometheusHttpServerBuilder
import io.opentelemetry.sdk.OpenTelemetrySdk
import io.opentelemetry.sdk.metrics.SdkMeterProvider
import io.opentelemetry.sdk.metrics.export.PeriodicMetricReader
import io.opentelemetry.sdk.resources.Resource
import io.opentelemetry.semconv.resource.attributes.ResourceAttributes
import java.time.Duration
import javax.sql.DataSource


public object Globals {

    public val dataSource: DataSource = HikariDataSource(
        HikariConfig().also {
            it.jdbcUrl = "jdbc:postgresql://localhost:5432/postgres"
            it.username = "postgres"
            it.password = "postgres"
            it.maximumPoolSize = 30
        }
    )

    public val prometheusServer: PrometheusHttpServer = PrometheusHttpServer.builder().setPort(8082).build()

    public val openTelemetry: OpenTelemetry = run {
        val resource = Resource.getDefault()
            .merge(Resource.create(Attributes.of(ResourceAttributes.SERVICE_NAME, "my-app")))

        val metricReader = prometheusServer

        val sdkMeterProvider = SdkMeterProvider.builder()
            .registerMetricReader(metricReader)
            .setResource(resource)
            .build()

        OpenTelemetrySdk.builder()
            .setMeterProvider(sdkMeterProvider)
            .build()
    }
}