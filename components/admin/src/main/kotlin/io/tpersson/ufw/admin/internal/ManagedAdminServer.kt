package io.tpersson.ufw.admin.internal

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import io.ktor.http.*
import io.ktor.serialization.jackson.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.http.content.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.callloging.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.tpersson.ufw.admin.AdminComponentConfig
import io.tpersson.ufw.admin.ApiException
import io.tpersson.ufw.managed.Managed
import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.slf4j.event.Level
import java.text.SimpleDateFormat

@Singleton
public class ManagedAdminServer @Inject constructor(
    private val config: AdminComponentConfig,
    private val adminModulesProvider: AdminModulesProvider,
) : Managed() {

    private var server: ApplicationEngine? = null

    override suspend fun onStarted() {
        server = embeddedServer(Netty, port = config.port) {
            install(ContentNegotiation) {
                jackson {
                    configureJackson(this)
                }
            }

            install(CallLogging) {
                level = Level.INFO
            }

            install(StatusPages) {
                exception<ApiException> { call, cause ->
                    call.respond(
                        status = cause.statusCode,
                        message = ApiErrorDTO(
                            errorCode = cause.errorCode,
                            errorMessage = cause.errorMessage
                        )
                    )
                }
                exception<Throwable> {call, cause ->
                    logger.error("Uncaught exception", cause)
                    call.respond(
                        status = HttpStatusCode.InternalServerError,
                        message = ApiErrorDTO(
                            errorCode = "internal.error",
                            errorMessage = "An unknown error occurred"
                        )
                    )
                }
            }

            adminModulesProvider.get().forEach {
                logger.info("Configuring AdminModule: ${it::class.simpleName}")
                it.configure(this)
            }

            routing {
                staticResources("/admin/ui", "/static-files") {
                    default("index.html")
                }
            }
        }

        server?.start(wait = false)
    }

    override suspend fun onStopped() {
        server?.stop()
        server = null
    }
}

public fun configureJackson(om: ObjectMapper) {
    om.configure(SerializationFeature.INDENT_OUTPUT, true)
    om.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
    om.findAndRegisterModules()
    om.dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ")
}

