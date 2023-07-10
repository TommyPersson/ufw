package io.tpersson.ufw.examples.common

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.opentelemetry.api.OpenTelemetry
import io.opentelemetry.api.common.Attributes
import io.opentelemetry.exporter.otlp.metrics.OtlpGrpcMetricExporter
import io.opentelemetry.sdk.OpenTelemetrySdk
import io.opentelemetry.sdk.metrics.SdkMeterProvider
import io.opentelemetry.sdk.metrics.export.PeriodicMetricReader
import io.opentelemetry.sdk.resources.Resource
import io.opentelemetry.semconv.resource.attributes.ResourceAttributes
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


    public val openTelemetry: OpenTelemetry = run {
        val resource = Resource.getDefault()
            .merge(Resource.create(Attributes.of(ResourceAttributes.SERVICE_NAME, "my-app")))

        val sdkMeterProvider = SdkMeterProvider.builder()
            .registerMetricReader(PeriodicMetricReader.builder(OtlpGrpcMetricExporter.builder().build()).build())
            .setResource(resource)
            .build()

        OpenTelemetrySdk.builder()
            .setMeterProvider(sdkMeterProvider)
            .build()
    }
}