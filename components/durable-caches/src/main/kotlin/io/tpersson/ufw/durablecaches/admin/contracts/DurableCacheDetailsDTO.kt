package io.tpersson.ufw.durablecaches.admin.contracts

public data class DurableCacheDetailsDTO(
    val id: String,
    val title: String,
    val description: String,
    val numEntries: Long,
)