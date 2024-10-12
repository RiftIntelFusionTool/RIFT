package dev.nohus.rift.pushover

import dev.nohus.rift.ViewModel
import dev.nohus.rift.compose.DialogMessage
import dev.nohus.rift.compose.MessageDialogType
import dev.nohus.rift.settings.persistence.Settings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.core.annotation.Single
import java.util.Locale

@Single
class PushoverViewModel(
    private val sendPushNotification: SendPushNotificationUseCase,
    private val settings: Settings,
) : ViewModel() {

    data class UiState(
        val dialogMessage: DialogMessage? = null,
        val apiToken: String,
        val userKey: String,
        val isLoading: Boolean = false,
    )

    private val _state = MutableStateFlow(
        UiState(
            apiToken = settings.pushover.apiToken ?: "",
            userKey = settings.pushover.userKey ?: "",
        ),
    )
    val state = _state.asStateFlow()

    fun onApiTokenChanged(token: String) {
        _state.update { it.copy(apiToken = token) }
        settings.pushover = settings.pushover.copy(apiToken = token)
    }

    fun onUserKeyChanged(key: String) {
        _state.update { it.copy(userKey = key) }
        settings.pushover = settings.pushover.copy(userKey = key)
    }

    private fun showDialog(title: String, message: String) {
        _state.update {
            it.copy(
                dialogMessage = DialogMessage(
                    title = title,
                    message = message,
                    type = MessageDialogType.Info,
                ),
            )
        }
    }

    fun onSendTest() {
        if (_state.value.isLoading) return

        val token = settings.pushover.apiToken
        val key = settings.pushover.userKey
        if (token.isNullOrEmpty()) {
            showDialog("No API Token", "You need to enter a Pushover API Token.")
            return
        }
        if (key.isNullOrEmpty()) {
            showDialog("No User Key", "You need to enter your Pushover User Key.")
            return
        }

        _state.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            val result = sendPushNotification(
                title = "RIFT Intel Fusion Tool",
                message = "Congratulations, RIFT is setup correctly for push notifications.",
            )

            val response = result.success
            if (response != null) {
                if (response.status == 1) {
                    showDialog("Success", "Notification sent successfully!")
                } else {
                    val reason = response.errors
                        ?.joinToString("\n") { it.replaceFirstChar { it.titlecase(Locale.US) } }
                        ?: "Unknown error"
                    showDialog("Failed to send", reason)
                }
            } else {
                showDialog("Failed to send", result.failure?.message ?: "Unknown error")
            }
            _state.update { it.copy(isLoading = false) }
        }
    }

    fun onCloseDialogMessage() {
        _state.update { it.copy(dialogMessage = null) }
    }
}
