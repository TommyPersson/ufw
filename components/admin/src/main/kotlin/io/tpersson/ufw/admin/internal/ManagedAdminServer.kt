package io.tpersson.ufw.admin.internal

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import io.ktor.serialization.jackson.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.http.content.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.callloging.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.routing.*
import io.tpersson.ufw.admin.AdminComponentConfig
import io.tpersson.ufw.admin.AdminModule
import io.tpersson.ufw.managed.Managed
import jakarta.inject.Inject
import org.slf4j.event.Level
import java.text.SimpleDateFormat

public class ManagedAdminServer @Inject constructor(
    private val config: AdminComponentConfig
) : Managed() {

    private var server: ApplicationEngine? = null

    override suspend fun onStarted() {
        val modules: List<AdminModule> = listOf(CoreAdminModule())

        val server = embeddedServer(Netty, port = config.port) {
            install(ContentNegotiation) {
                jackson {
                    configureJackson(this)
                }
            }

            install(CallLogging) {
                level = Level.INFO
            }

            modules.forEach {
                it.configure(this)
            }

            routing {
                staticResources("/admin/ui", "/static-files") {
                    default("index.html")
                }
            }
        }

        server.start(wait = false)
    }

    override suspend fun onStopped() {
        server?.stop()
    }
}

public fun configureJackson(om: ObjectMapper) {
    om.configure(SerializationFeature.INDENT_OUTPUT, true)
    om.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
    om.findAndRegisterModules()
    om.dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ")
}