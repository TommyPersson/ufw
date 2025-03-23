package io.tpersson.ufw.durablecaches.admin

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.tpersson.ufw.admin.AdminModule
import io.tpersson.ufw.admin.contracts.toDTO
import io.tpersson.ufw.admin.utils.getPaginationOptions
import io.tpersson.ufw.durablecaches.CacheEntry
import io.tpersson.ufw.durablecaches.DurableCacheDefinition
import io.tpersson.ufw.durablecaches.admin.contracts.DurableCacheDetailsDTO
import io.tpersson.ufw.durablecaches.admin.contracts.DurableCacheEntryItemDTO
import io.tpersson.ufw.durablecaches.admin.contracts.DurableCacheItemDTO
import jakarta.inject.Inject
import jakarta.inject.Singleton

@Singleton
public class DurableCachesAdminModule @Inject constructor(
    private val adminFacade: DurableCachesAdminFacade,
) : AdminModule {
    override val moduleId: String = "durable-caches"

    override fun configure(application: Application) {
        application.routing {
            get("/admin/api/durable-caches/caches") {
                val paginationOptions = call.getPaginationOptions()

                val caches = adminFacade.listCaches(paginationOptions).toDTO {
                    it.toItemDTO(adminFacade.getNumEntries(it.id))
                }

                call.respond(caches)
            }

            get("/admin/api/durable-caches/caches/{cacheId}") {
                val cacheId = call.parameters.cacheId!!

                val cache = adminFacade.getByCacheId(cacheId)
                    ?.toDetailsDTO(adminFacade.getNumEntries(cacheId))

                if (cache == null) {
                    call.respond(HttpStatusCode.NotFound)
                    return@get
                }

                call.respond(cache)
            }

            post("/admin/api/durable-caches/caches/{cacheId}/actions/invalidate-all") {
                val cacheId = call.parameters.cacheId!!

                adminFacade.invalidateAllEntries(cacheId)

                call.respond(HttpStatusCode.NoContent)
            }

            get("/admin/api/durable-caches/caches/{cacheId}/entries") {
                val paginationOptions = call.getPaginationOptions()
                val cacheId = call.parameters.cacheId!!
                val keyPrefix = call.parameters["keyPrefix"]!!

                val entries = adminFacade.listEntries(cacheId, keyPrefix, paginationOptions).toDTO { it.toItemDTO() }

                call.respond(entries)
            }

            post("/admin/api/durable-caches/caches/{cacheId}/entries/{cacheKey}/actions/invalidate") {
                val cacheId = call.parameters.cacheId!!
                val cacheKey = call.parameters.cacheKey!!

                adminFacade.invalidateEntry(cacheId, cacheKey)

                call.respond(HttpStatusCode.NoContent)
            }
        }
    }
}

private val Parameters.cacheId: String? get() = this["cacheId"]
private val Parameters.cacheKey: String? get() = this["cacheKey"]

private fun DurableCacheDefinition<*>.toItemDTO(numEntries: Long): DurableCacheItemDTO {
    return DurableCacheItemDTO(
        id = id,
        title = title,
        description = description,
        numEntries = numEntries,
    )
}

private fun DurableCacheDefinition<*>.toDetailsDTO(numEntries: Long): DurableCacheDetailsDTO {
    return DurableCacheDetailsDTO(
        id = id,
        title = title,
        description = description,
        numEntries = numEntries,
    )
}

private fun CacheEntry<*>.toItemDTO(): DurableCacheEntryItemDTO {
    return DurableCacheEntryItemDTO(
        key = key,
        cachedAt = cachedAt,
        expiresAt = expiresAt,
    )
}