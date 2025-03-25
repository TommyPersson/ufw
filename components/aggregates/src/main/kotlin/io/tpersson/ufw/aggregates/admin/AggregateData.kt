package io.tpersson.ufw.aggregates.admin

public data class AggregateData(
    val id: String,
    val type: String,
    val version: Long,
    val json: String,
    val factTypes: List<FactType>,
) {
    public data class FactType(
        val type: String,
    )
}