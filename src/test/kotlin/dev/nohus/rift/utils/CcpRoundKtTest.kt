package dev.nohus.rift.utils

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe

class CcpRoundKtTest : FreeSpec({

    listOf(
        -1.0 to -1.0,
        -0.951 to -1.0,
        -0.95 to -0.9,
        -0.949 to -0.9,
        -0.94 to -0.9,
        -0.9 to -0.9,
        -0.01 to 0.0,
        0.0 to 0.0,
        0.01 to 0.1,
        0.05 to 0.1,
        0.15 to 0.2,
        0.949 to 0.9,
        0.95 to 1.0,
        1.0 to 1.0,
    ).forEach { (input, expected) ->
        "System security $input rounds to $expected" {
            input.roundSecurity() shouldBe expected
        }
    }
})
