package dev.nohus.rift.network.pushover

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Messages(
    @SerialName("token")
    val token: String,
    @SerialName("user")
    val user: String,
    @SerialName("title")
    val title: String,
    @SerialName("message")
    val message: String,
    @SerialName("attachment_type")
    val attachmentType: String? = null,
    @SerialName("attachment_base64")
    val attachmentBase64: String? = null,
)

@Serializable
data class MessagesResponse(
    @SerialName("status")
    val status: Int,
    @SerialName("request")
    val request: String,
    @SerialName("errors")
    val errors: List<String>? = null,
)
