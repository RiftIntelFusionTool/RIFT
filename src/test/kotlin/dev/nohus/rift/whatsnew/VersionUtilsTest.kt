package dev.nohus.rift.whatsnew

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe

class VersionUtilsTest : FreeSpec({

    listOf(
        Triple("0.0.0", "0.0.1", true),
        Triple("0.0.0", "0.1.0", true),
        Triple("0.0.0", "1.0.0", true),
        Triple("5.5.5", "5.5.6", true),
        Triple("5.5.5", "5.6.5", true),
        Triple("5.5.5", "6.5.5", true),
        Triple("5.5.5", "6.6.6", true),
        Triple("5.5.5", "5.5.5", false),
        Triple("5.5.5", "5.5.4", false),
        Triple("5.5.5", "5.4.5", false),
        Triple("5.5.5", "4.5.5", false),
        Triple("5.5.5", "4.4.4", false),
        Triple("0.0.0", "0.0", false),
        Triple("0.0", "0.0.0", false),
    ).forEach { (a, b, isNewer) ->
        "Version $b is newer than $a: $isNewer" {
            VersionUtils.isNewer(a, b).shouldBe(isNewer)
        }
    }
})
