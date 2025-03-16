package io.tpersson.ufw.core.utils

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

public data class PaginationOptions(
    val limit: Int = 1000,
    val offset: Int = 0,
) {
    public companion object {
        public val DEFAULT: PaginationOptions = PaginationOptions()
    }
}

public data class PaginatedList<TItem>(
    val items: List<TItem>,
    val options: PaginationOptions,
    val hasMoreItems: Boolean,
) {
    public companion object {
        public fun <TItem> empty(options: PaginationOptions?): PaginatedList<TItem> {
            return PaginatedList(
                items = emptyList(),
                options = options ?: PaginationOptions.DEFAULT,
                hasMoreItems = false
            )
        }
    }

    public fun <R> map(block: (TItem) -> R): PaginatedList<R> {
        return PaginatedList(items.map(block), options, hasMoreItems)
    }
}

public fun <TItem> paginate(
    initialPaginationOptions: PaginationOptions = PaginationOptions.DEFAULT,
    block: suspend (paginationOptions: PaginationOptions) -> PaginatedList<TItem>
): Flow<PaginatedList<TItem>> {
    return flow {
        var options = initialPaginationOptions
        do {
            val page = block(options)
            emit(page)
            options = page.options.copy(offset = options.offset + options.limit)
        } while (page.hasMoreItems)
    }
}