package dev.nohus.rift.utils.directories

import org.koin.core.annotation.Single
import java.nio.file.Path
import kotlin.io.path.readText

@Single
class GetLinuxPartitionsUseCase {

    data class Mount(val device: String, val mount: String, val type: String)

    private val mountsFile = Path.of("/proc/mounts")
    private val ignoredDevices = listOf("sysfs", "proc", "udev", "tmpfs", "ramfs", "overlay")
    private val ignoredMountsRegex = "/(sys|proc|dev|run|boot|var|tmp)[^ ]*".toRegex()
    private val ignoredTypes = listOf("squashfs")

    /**
     * Returns a list partition roots, ignoring system and technical mounts
     */
    operator fun invoke(): List<Path> {
        return mountsFile.readText().trim().lines().map { line ->
            val (device, mount, type) = line.split(" ")
            Mount(device, mount, type)
        }.filter {
            if (it.device in ignoredDevices) return@filter false
            if (it.type in ignoredTypes) return@filter false
            if (it.mount.matches(ignoredMountsRegex)) return@filter false
            true
        }.map {
            Path.of(it.mount)
        }
    }
}
