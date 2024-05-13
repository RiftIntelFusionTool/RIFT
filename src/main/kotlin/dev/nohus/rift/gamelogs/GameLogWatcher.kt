package dev.nohus.rift.gamelogs

import dev.nohus.rift.characters.repositories.OnlineCharactersRepository
import dev.nohus.rift.logs.GameLogsObserver
import dev.nohus.rift.logs.GetGameLogsDirectoryUseCase
import dev.nohus.rift.settings.persistence.Settings
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import org.koin.core.annotation.Single
import java.nio.file.Path

private val logger = KotlinLogging.logger {}

@Single
class GameLogWatcher(
    private val gameLogsObserver: GameLogsObserver,
    private val getGameLogsDirectoryUseCase: GetGameLogsDirectoryUseCase,
    private val settings: Settings,
    private val onlineCharactersRepository: OnlineCharactersRepository,
    private val gameLogMessageParser: GameLogMessageParser,
) {

    private var watchedDirectory: Path? = null
    private var isWatchedDirectoryInitialized = false

    suspend fun start() = coroutineScope {
        launch {
            observeGameLogs()
        }
        launch {
            settings.updateFlow.collect {
                settings.eveLogsDirectory.let { new ->
                    if (isWatchedDirectoryInitialized && new != watchedDirectory) {
                        launch {
                            observeGameLogs()
                        }
                    }
                }
            }
        }
    }

    private suspend fun observeGameLogs() {
        gameLogsObserver.stop()
        val logsDirectory = settings.eveLogsDirectory
        val gameLogsDirectory = getGameLogsDirectoryUseCase(logsDirectory)
        if (gameLogsDirectory != null) {
            watchedDirectory = logsDirectory
            isWatchedDirectoryInitialized = true
            observeGameLogs(gameLogsDirectory)
        } else {
            watchedDirectory = null
            isWatchedDirectoryInitialized = true
            logger.warn { "No game logs directory detected" }
        }
    }

    private suspend fun observeGameLogs(directory: Path) {
        gameLogsObserver.observe(
            directory = directory,
            onCharacterLogin = ::onCharacterLogin,
            onMessage = gameLogMessageParser::onMessage,
        )
    }

    private suspend fun onCharacterLogin(characterId: Int) {
        logger.info { "Logged in: $characterId" }
        onlineCharactersRepository.onCharacterLogin(characterId)
    }
}
