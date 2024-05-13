package dev.nohus.rift.database

import dev.nohus.rift.database.static.StaticDatabase
import dev.nohus.rift.repositories.SolarSystemsRepository
import dev.nohus.rift.utils.OperatingSystem
import dev.nohus.rift.utils.directories.AppDirectories
import dev.nohus.rift.utils.osdirectories.LinuxDirectories
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe

class SolarSystemsRepositoryTest : FreeSpec({

    val target = SolarSystemsRepository(StaticDatabase(SqliteInitializer(OperatingSystem.Linux, AppDirectories(LinuxDirectories()))))

    listOf(
        Triple("Jita", null, "Jita"),
        Triple("Jita", "Delve", "Jita"),
        Triple("jita", null, "Jita"),
        Triple("1DQ1-A", null, "1DQ1-A"),
        Triple("1dq", null, "1DQ1-A"),
        Triple("1DQ", null, "1DQ1-A"),
        Triple("1dq1", null, "1DQ1-A"),
        Triple("1dQ1-a", null, "1DQ1-A"),
        Triple("invalid", null, null),
        Triple("", null, null),
        Triple("RP", null, null),
        Triple("rp", null, null),
        Triple("1dq1-", null, null),
        Triple("Jita ", null, null),
        Triple(" Jita", null, null),
        Triple("40-239", null, "4O-239"),
        Triple("O-3VW8", null, "0-3VW8"),
        Triple("4O-", "Outer Passage", "4O-ZRI"),
        Triple("4O-", "Delve", "4O-239"),
        Triple("4O-", "Period Basis", null),
        Triple("4O-", null, null),
        Triple("40-", "Outer Passage", "4O-ZRI"),
        Triple("40-", "Delve", "4O-239"),
        Triple("40-", "Period Basis", null),
        Triple("40-", null, null),
        Triple("1B", "Delve", "1B-VKF"),
        Triple("1B", "Tenal", "1BWK-S"),
        Triple("1B", null, null),
        Triple("1d", null, null),
        Triple("1D", null, null),
        Triple("1d", "Delve", null),
        Triple("1D", "Delve", null),
        Triple("1d", "Paragon Soul", "1DDR-X"),
        Triple("1D", "Paragon Soul", "1DDR-X"),
    ).forEach { (input, regionHint, expected) ->
        "for input \"$input\" with region hint \"$regionHint\" getSystem() returns \"$expected\"" {
            val actual = target.getSystemName(input, regionHint)
            actual shouldBe expected
        }
    }
})
