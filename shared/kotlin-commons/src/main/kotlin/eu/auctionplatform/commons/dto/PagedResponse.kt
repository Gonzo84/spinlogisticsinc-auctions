package eu.auctionplatform.commons.dto

/**
 * Generic paginated response wrapper.
 *
 * @param T         The type of items in the page.
 * @property items    The items in the current page.
 * @property total    Total number of items across all pages.
 * @property page     Current page number (1-based).
 * @property pageSize Number of items requested per page.
 */
data class PagedResponse<T>(
    val items: List<T>,
    val total: Long,
    val page: Int,
    val pageSize: Int
) {
    /** Total number of pages, derived from [total] and [pageSize]. */
    val totalPages: Int
        get() = if (pageSize > 0) ((total + pageSize - 1) / pageSize).toInt() else 0

    /** Whether there is a next page. */
    val hasNext: Boolean
        get() = page < totalPages

    /** Whether there is a previous page. */
    val hasPrevious: Boolean
        get() = page > 1
}
