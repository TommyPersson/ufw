package io.tpersson.ufw.admin.contracts

import io.tpersson.ufw.core.utils.PaginatedList

public data class PaginatedListDTO<TItem>(
    val items: List<TItem>,
    val hasMoreItems: Boolean,
)


public fun <TItem, TItemDTO> PaginatedList<TItem>.toDTO(transform: (TItem) -> TItemDTO): PaginatedListDTO<TItemDTO> {
    return PaginatedListDTO(
        items = items.map(transform),
        hasMoreItems = hasMoreItems,
    )
}

