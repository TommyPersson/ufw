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
import io.ktor.server.plugins.calllogging.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.tpersson.ufw.admin.ApiException
import io.tpersson.ufw.admin.HttpException
import io.tpersson.ufw.admin.configuration.Admin
import io.tpersson.ufw.core.configuration.ConfigProvider
import io.tpersson.ufw.core.configuration.Configs
import io.tpersson.ufw.managed.Managed
import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.slf4j.event.Level
import java.text.SimpleDateFormat

@Singleton
public class ManagedAdminServer @Inject constructor(
    private val adminModulesRegistry: AdminModulesRegistry,
    private val configProvider: ConfigProvider
) : Managed() {

    private val serverPort = configProvider.get(Configs.Admin.ServerPort)

    private var server: EmbeddedServer<*, *>? = null

    override suspend fun onStarted() {
        if (serverPort == null) {
            logger.info("Admin ServerPort not configured, exiting!")
            return
        }

        server = embeddedServer(Netty, port = serverPort) {
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
                exception<HttpException> { call, cause ->
                    call.respond(
                        status = cause.statusCode,
                        message = ApiErrorDTO(
                            errorCode = "http.${cause.statusCode.value}",
                            errorMessage = cause.statusText
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

            adminModulesRegistry.get().forEach {
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
    om.disable(SerializationFeature.WRITE_DURATIONS_AS_TIMESTAMPS)
    om.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
    om.findAndRegisterModules()
    om.dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ")
}

