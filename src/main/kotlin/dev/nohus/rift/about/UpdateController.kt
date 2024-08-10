package dev.nohus.rift.about

import dev.hydraulic.conveyor.control.SoftwareUpdateController
import dev.hydraulic.conveyor.control.SoftwareUpdateController.Availability.AVAILABLE
import dev.hydraulic.conveyor.control.SoftwareUpdateController.UpdateCheckException
import dev.hydraulic.conveyor.control.SoftwareUpdateController.Version
import dev.nohus.rift.about.UpdateController.UpdateAvailability.NOT_PACKAGED
import dev.nohus.rift.about.UpdateController.UpdateAvailability.NO_UPDATE
import dev.nohus.rift.about.UpdateController.UpdateAvailability.UNKNOWN
import dev.nohus.rift.about.UpdateController.UpdateAvailability.UPDATE_AUTOMATIC
import dev.nohus.rift.about.UpdateController.UpdateAvailability.UPDATE_MANUAL
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.koin.core.annotation.Single

private val logger = KotlinLogging.logger {}

@Single
class UpdateController {

    private val controller: SoftwareUpdateController? = SoftwareUpdateController.getInstance()

    enum class UpdateAvailability {
        NOT_PACKAGED, UNKNOWN, NO_UPDATE, UPDATE_MANUAL, UPDATE_AUTOMATIC
    }

    suspend fun isUpdateAvailable(): UpdateAvailability = withContext(Dispatchers.IO) {
        if (controller != null) {
            val currentVersion: Version? = controller.currentVersion
            if (currentVersion != null) {
                try {
                    val latestVersion = controller.currentVersionFromRepository
                    if (latestVersion > currentVersion) {
                        val canTriggerUpdate = controller.canTriggerUpdateCheckUI()
                        if (canTriggerUpdate == AVAILABLE) {
                            return@withContext UPDATE_AUTOMATIC
                        } else {
                            logger.warn { "Can not trigger update UI: $canTriggerUpdate" }
                            return@withContext UPDATE_MANUAL
                        }
                    } else {
                        return@withContext NO_UPDATE
                    }
                } catch (e: UpdateCheckException) {
                    logger.warn { "Conveyor could not check latest version: ${e.message}" }
                }
            } else {
                logger.warn { "Conveyor could not determine the current version" }
            }
        } else {
            return@withContext NOT_PACKAGED
        }
        return@withContext UNKNOWN
    }

    fun triggerUpdate() {
        controller?.triggerUpdateCheckUI()
    }
}
