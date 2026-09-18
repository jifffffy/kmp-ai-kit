package dev.kmpapp.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class NetworkErrorModel(
    @SerialName("detail")
    val detailMessage: String?,
    @SerialName("code")
    val errorCode: Int?,
)
