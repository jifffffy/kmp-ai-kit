package dev.kmpapp.data.model

import kotlinx.serialization.Serializable

/**
 * Generic pagination result model used across the application
 * Consolidated from OrdersResponse and ProductsResponse
 * Created by Ali Sadeghi on 29,Dec,2024
 */

@Serializable
data class PaginationResult<T>(
    val count: Int?,
    val next: String?,
    val previous: String?,
    val results: List<T>? = emptyList(),
) {
    /**
     * Get results as non-null list, defaulting to empty list if null
     */
    fun safeResults(): List<T> = results ?: emptyList()
}
