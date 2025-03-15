package io.tpersson.ufw.examples.common

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
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

    public val meterRegistry: MeterRegistry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)
}