package io.tpersson.ufw.durablecaches.admin.contracts

import io.tpersson.ufw.admin.contracts.ApplicationModuleDTO
import java.time.Duration


public data class DurableCacheDetailsDTO(
    val id: String,
    val title: String,
    val description: String,
    val containsSensitiveData: Boolean,
    val expirationDuration: Duration?,
    val inMemoryExpirationDuration: Duration?,
    val numEntries: Long,
    val applicationModule: ApplicationModuleDTO
)