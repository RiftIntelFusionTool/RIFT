package dev.nohus.rift.utils

import io.github.oshai.kotlinlogging.KotlinLogging
import org.apache.commons.exec.CommandLine
import org.apache.commons.exec.DefaultExecutor
import org.apache.commons.exec.ExecuteException
import org.apache.commons.exec.ExecuteResultHandler
import org.apache.commons.exec.ExecuteWatchdog
import org.apache.commons.exec.PumpStreamHandler
import org.koin.core.annotation.Single
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.OutputStream
import java.time.Duration

private val logger = KotlinLogging.logger {}

@Single
class CommandRunner {

    fun run(command: String, vararg arguments: String, ignoreFailure: Boolean = false, timeout: Long = 10_000): CommandResult {
        val commandLine = CommandLine.parse(command)
        arguments.forEach { commandLine.addArgument(it, false) }
        val executor = DefaultExecutor.Builder().get()
        val watchdog = ExecuteWatchdog.Builder().setTimeout(Duration.ofMillis(timeout)).get()
        executor.watchdog = watchdog
        val outputStream = ByteArrayOutputStream()
        executor.streamHandler = PumpStreamHandler(outputStream)
        val exitValue = try {
            executor.execute(commandLine)
        } catch (e: ExecuteException) {
            e.exitValue
        } catch (e: IOException) {
            -1
        }

        if (watchdog.killedProcess()) {
            logger.error { "Command \"$command\" timed out, output: $outputStream" }
            if (ignoreFailure) {
                return CommandResult("", -1)
            } else {
                throw IllegalStateException("Command \"$command\" has timed out.")
            }
        }

        if (exitValue < 0 && !ignoreFailure) {
            logger.error { "Command \"$command\" failed, output: $outputStream" }
            throw IllegalStateException("Command \"$command\" has failed.")
        }

        return CommandResult(
            output = outputStream.toString(),
            exitStatus = exitValue,
        )
    }

    fun runAsync(command: String, vararg arguments: String) {
        val commandLine = CommandLine.parse(command)
        arguments.forEach { commandLine.addArgument(it, false) }
        val executor = DefaultExecutor.Builder().get()
        executor.streamHandler = PumpStreamHandler(OutputStream.nullOutputStream())
        val handler = object : ExecuteResultHandler {
            override fun onProcessComplete(exitValue: Int) {}
            override fun onProcessFailed(e: ExecuteException?) {}
        }
        try {
            executor.execute(commandLine, handler)
        } catch (e: ExecuteException) {
            logger.error(e) { "Failed to execute: $commandLine" }
        } catch (e: IOException) {
            logger.error(e) { "Failed to execute: $commandLine" }
        }
    }

    data class CommandResult(
        val output: String,
        val exitStatus: Int,
    )
}
