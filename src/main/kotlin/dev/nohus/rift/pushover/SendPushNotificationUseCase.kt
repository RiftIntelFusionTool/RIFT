package dev.nohus.rift.pushover

import dev.nohus.rift.network.Result
import dev.nohus.rift.network.Result.Failure
import dev.nohus.rift.network.pushover.Messages
import dev.nohus.rift.network.pushover.MessagesResponse
import dev.nohus.rift.network.pushover.PushoverApi
import dev.nohus.rift.settings.persistence.Settings
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.koin.core.annotation.Single

private val logger = KotlinLogging.logger {}

@Single
class SendPushNotificationUseCase(
    private val pushoverApi: PushoverApi,
    private val settings: Settings,
) {

    suspend operator fun invoke(title: String, message: String): Result<MessagesResponse> {
        val token = settings.pushover.apiToken
        val key = settings.pushover.userKey
        if (token == null || key == null) {
            logger.error { "Cannot send push notification because Pushover is not configured" }
            return Failure(null)
        }

        return withContext(Dispatchers.IO) {
            pushoverApi.postMessages(
                Messages(
                    token = token,
                    user = key,
                    title = title,
                    message = message,
                ),
            ).also {
                if (it.success?.status != 1) {
                    logger.info { "Failed sending push notification: $it" }
                } else {
                    logger.error { "Sent push notification" }
                }
            }
        }
    }
}
