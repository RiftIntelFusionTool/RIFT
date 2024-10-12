package dev.nohus.rift

import dev.nohus.rift.alerts.AlertsTriggerController
import dev.nohus.rift.alerts.PlanetaryInteractionAlertTriggerController
import dev.nohus.rift.assets.AssetsRepository
import dev.nohus.rift.autopilot.AutopilotController
import dev.nohus.rift.characters.repositories.ActiveCharacterRepository
import dev.nohus.rift.characters.repositories.CharacterWalletRepository
import dev.nohus.rift.characters.repositories.LocalCharactersRepository
import dev.nohus.rift.characters.repositories.OnlineCharactersRepository
import dev.nohus.rift.clipboard.Clipboard
import dev.nohus.rift.clones.ClonesRepository
import dev.nohus.rift.gamelogs.GameLogWatcher
import dev.nohus.rift.intel.ChatLogWatcher
import dev.nohus.rift.jabber.client.StartJabberUseCase
import dev.nohus.rift.location.CharacterLocationRepository
import dev.nohus.rift.logging.analytics.Analytics
import dev.nohus.rift.map.MapJumpRangeController
import dev.nohus.rift.network.killboard.KillboardObserver
import dev.nohus.rift.pings.PingsRepository
import dev.nohus.rift.planetaryindustry.PlanetaryIndustryRepository
import dev.nohus.rift.repositories.MapStatusRepository
import dev.nohus.rift.settings.persistence.Settings
import dev.nohus.rift.standings.StandingsRepository
import dev.nohus.rift.utils.ResetSparkleUpdateCheckUseCase
import dev.nohus.rift.utils.sound.SoundPlayer
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import org.koin.core.annotation.Single

@Single
class BackgroundProcesses(
    private val onlineCharactersRepository: OnlineCharactersRepository,
    private val localCharactersRepository: LocalCharactersRepository,
    private val characterLocationRepository: CharacterLocationRepository,
    private val activeCharacterRepository: ActiveCharacterRepository,
    private val characterWalletRepository: CharacterWalletRepository,
    private val gameLogWatcher: GameLogWatcher,
    private val chatLogsWatcher: ChatLogWatcher,
    private val alertsTriggerController: AlertsTriggerController,
    private val resetSparkleUpdateCheckUseCase: ResetSparkleUpdateCheckUseCase,
    private val startJabberUseCase: StartJabberUseCase,
    private val pingsRepository: PingsRepository,
    private val killboardObserver: KillboardObserver,
    private val soundPlayer: SoundPlayer,
    private val clipboard: Clipboard,
    private val autopilotController: AutopilotController,
    private val assetsRepository: AssetsRepository,
    private val mapStatusRepository: MapStatusRepository,
    private val mapJumpRangeController: MapJumpRangeController,
    private val analytics: Analytics,
    private val standingsRepository: StandingsRepository,
    private val clonesRepository: ClonesRepository,
    private val planetaryIndustryRepository: PlanetaryIndustryRepository,
    private val planetaryInteractionAlertTriggerController: PlanetaryInteractionAlertTriggerController,
    private val settings: Settings,
) {

    suspend fun start() = supervisorScope {
        launch {
            resetSparkleUpdateCheckUseCase()
        }
        if (!settings.isDemoMode) {
            launch {
                localCharactersRepository.load()
                localCharactersRepository.start()
            }
            launch {
                gameLogWatcher.start()
            }
            launch {
                onlineCharactersRepository.start()
            }
            launch {
                characterLocationRepository.start()
            }
            launch {
                activeCharacterRepository.start()
            }
            launch {
                characterWalletRepository.start()
            }
            launch {
                pingsRepository.start()
            }
            launch {
                chatLogsWatcher.start()
            }
            launch {
                alertsTriggerController.start()
            }
            launch {
                startJabberUseCase()
            }
            launch {
                killboardObserver.start()
            }
            launch {
                soundPlayer.start()
            }
            launch {
                clipboard.start()
            }
            launch {
                autopilotController.start()
            }
            launch {
                assetsRepository.start()
            }
            launch {
                mapStatusRepository.start()
            }
            launch {
                mapJumpRangeController.start()
            }
            launch {
                analytics.start()
            }
            launch {
                standingsRepository.start()
            }
            launch {
                clonesRepository.start()
            }
            launch {
                planetaryIndustryRepository.start()
            }
            launch {
                planetaryInteractionAlertTriggerController.start()
            }
        }
    }
}
