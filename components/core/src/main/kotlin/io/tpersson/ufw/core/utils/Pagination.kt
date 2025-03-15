package io.tpersson.ufw.core.utils

public data class PaginationOptions(
    val limit: Int = 100,
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
)