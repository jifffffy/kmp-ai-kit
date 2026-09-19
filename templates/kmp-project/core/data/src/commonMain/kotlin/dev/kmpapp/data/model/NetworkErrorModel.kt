package dev.kmpapp.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// Nullable is NOT enough: kotlinx.serialization treats a property without a default as
// REQUIRED, even when its type is nullable. Without `= null` the adapter threw
// MissingFieldException on any error body that omitted `detail` or `code` — which is most
// of them. The EitherCallAdapter then collapsed every failure (404/429/500) into a generic
// network error, so status-specific error mapping was impossible to implement.
@Serializable
data class NetworkErrorModel(
    @SerialName("detail")
    val detailMessage: String? = null,
    @SerialName("code")
    val errorCode: Int? = null,
)
