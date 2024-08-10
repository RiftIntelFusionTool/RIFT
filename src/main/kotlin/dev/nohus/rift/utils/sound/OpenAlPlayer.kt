package dev.nohus.rift.utils.sound

import com.jogamp.openal.AL
import com.jogamp.openal.ALException
import com.jogamp.openal.ALFactory
import com.jogamp.openal.util.ALut
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.delay
import java.io.InputStream
import java.nio.ByteBuffer

private val logger = KotlinLogging.logger {}

class OpenAlPlayer {

    private var openAl: AL? = null

    init {
        try {
            openAl = initializeOpenAl()
        } catch (e: ALException) {
            logger.error(e) { "Could not initialize OpenAL, sound playback won't work" }
        } catch (e: Throwable) {
            logger.error(e) { "Could not initialize OpenAL: $e" }
        }
    }

    fun shutdown() {
        ALut.alutExit()
    }

    suspend fun play(
        inputStream: InputStream,
        gain: Float,
    ) {
        val openAl = openAl ?: return

        val buffer = IntArray(1) // Buffer holds sound data
        val source = IntArray(1) // Source is the point emitting sound

        openAl.alGenSources(1, source, 0)
        if (openAl.alGetError() != AL.AL_NO_ERROR) throw ALException("Error generating OpenAL source")

        try {
            loadWav(buffer, inputStream)
            setupSource(buffer, source, gain)
            openAl.alSourcePlay(source[0])
            val state = IntArray(1)
            do {
                delay(50)
                openAl.alGetSourcei(source[0], AL.AL_SOURCE_STATE, state, 0)
            } while (state[0] == AL.AL_PLAYING)
            clearData(buffer, source)
        } catch (e: ALException) {
            logger.error(e) { "Could not play audio" }
        }
    }

    private fun initializeOpenAl(): AL {
        ALut.alutInit()
        return ALFactory.getAL().also { it.alGetError() }
    }

    private fun loadWav(
        buffer: IntArray,
        inputStream: InputStream,
    ) {
        val openAl = openAl ?: return

        val format = IntArray(1)
        val size = IntArray(1)
        val data = arrayOfNulls<ByteBuffer>(1)
        val freq = IntArray(1)
        val loop = IntArray(1)

        // Load wav data into a buffer
        openAl.alGenBuffers(1, buffer, 0)
        if (openAl.alGetError() != AL.AL_NO_ERROR) throw ALException("Error generating OpenAL buffers")

        ALut.alutLoadWAVFile(inputStream, format, data, size, freq, loop)
        if (data[0] == null) throw ALException("Error loading WAV file")
        openAl.alBufferData(buffer[0], format[0], data[0], size[0], freq[0])
    }

    private fun setupSource(
        buffer: IntArray,
        source: IntArray,
        gain: Float,
    ) {
        val openAl = openAl ?: return

        openAl.alSourcei(source[0], AL.AL_BUFFER, buffer[0])
        openAl.alSourcef(source[0], AL.AL_PITCH, 1.0f)
        openAl.alSourcef(source[0], AL.AL_GAIN, gain)
        if (openAl.alGetError() != AL.AL_NO_ERROR) throw ALException("Error setting up OpenAL source")
    }

    private fun clearData(
        buffer: IntArray,
        source: IntArray,
    ) {
        val openAl = openAl ?: return

        openAl.alDeleteSources(1, source, 0)
        openAl.alDeleteBuffers(1, buffer, 0)
    }
}
