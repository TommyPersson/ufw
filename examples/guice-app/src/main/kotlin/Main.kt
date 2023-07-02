import com.google.inject.Guice
import com.google.inject.Module
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.tpersson.ufw.db.guice.DbGuiceModule
import io.tpersson.ufw.keyvaluestore.KeyValueStore
import io.tpersson.ufw.keyvaluestore.guice.KeyValueStoreGuiceModule
import javax.sql.DataSource

public suspend fun main() {

    val hikariConfig = HikariConfig().also {
        it.jdbcUrl = "jdbc:postgresql://localhost:5432/postgres"
        it.username = "postgres"
        it.password = "postgres"
        it.maximumPoolSize = 30
    }

    val dataSource = HikariDataSource(hikariConfig)

    val injector = Guice.createInjector(
        Module { it.bind(DataSource::class.java).toInstance(dataSource) },
        DbGuiceModule(),
        KeyValueStoreGuiceModule()
    )

    val keyValueStore = injector.getInstance(KeyValueStore::class.java)

    val key = KeyValueStore.Key.of<String>("greeting")

    keyValueStore.put(key, "Hello, World!")

    val greeting = keyValueStore.get(key)?.value ?: "Missing Value"

    println(greeting)
}