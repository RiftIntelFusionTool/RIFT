package dev.nohus.rift.logs.parse

import io.kotest.core.spec.IsolationMode
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe

class CharacterNameValidatorTest : FreeSpec({

    isolationMode = IsolationMode.InstancePerTest

    val target = CharacterNameValidator()

    listOf(
        "" to false, // Empty
        "12" to false, // Too short
        "12345678901234567890 1234567890 1234567890" to false, // Too long
        "12345678901234567890123456 1 1" to false, // First name too long
        "1 1 1234567890123" to false, // Last name too long
        "123+" to false, // Invalid character
        "1-2'3" to true,
        "Nohus Bluxome" to true,
    ).forEach { (name, expected) ->
        "Character name \"$name\" is valid: $expected" {
            val actual = target.isValid(name)

            actual shouldBe expected
        }
    }
})
