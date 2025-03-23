package io.tpersson.ufw.admin.contracts

import io.tpersson.ufw.core.utils.PaginatedList

public data class PaginatedListDTO<TItem>(
    val items: List<TItem>,
    val hasMoreItems: Boolean,
)


public suspend fun <TItem, TItemDTO> PaginatedList<TItem>.toDTO(transform: suspend (TItem) -> TItemDTO): PaginatedListDTO<TItemDTO> {
    return PaginatedListDTO(
        items = items.map { transform(it) },
        hasMoreItems = hasMoreItems,
    )
}

