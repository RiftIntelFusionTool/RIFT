package dev.nohus.rift.utils.sound

import dev.nohus.rift.generated.resources.Res
import dev.nohus.rift.settings.persistence.Settings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.launch
import org.koin.core.annotation.Single
import java.io.File
import java.io.InputStream
import java.util.concurrent.Executors

@Single
class SoundPlayer(
    private val openAlPlayer: OpenAlPlayer,
    private val settings: Settings,
) {

    private val dispatcher = Executors.newCachedThreadPool().asCoroutineDispatcher()
    private val scope = CoroutineScope(Job() + dispatcher)

    fun play(soundResource: String) = scope.launch {
        val inputStream = Res.readBytes("files/sounds/$soundResource").inputStream()
        play(inputStream)
    }

    fun playFile(path: String) = scope.launch {
        val file = File(path)
        if (!file.exists() || !file.canRead()) return@launch
        play(file.inputStream())
    }

    private suspend fun play(inputStream: InputStream) {
        val gain = settings.soundsVolume / 100f
        openAlPlayer.play(inputStream, gain)
    }
}
