package dev.nohus.rift.logs.parse

import org.koin.core.annotation.Single

@Single
class CharacterNameValidator {

    fun isValid(name: String): Boolean {
        return PLAYER_NAME_REGEX.matches(name) && PLAYER_NAME_REGEX2.matches(name) && PLAYER_NAME_REGEX3.matches(name)
    }

    fun getInvalidReason(name: String): String {
        return when {
            !PLAYER_NAME_REASON_CHARACTERS_REGEX.matches(name) ->
                "EVE character names may only contain letters, digits, spaces, \"'\", and \"-\""
            !PLAYER_NAME_REASON_LENGTH_REGEX.matches(name) ->
                "EVE character names must be between 3 and 37 characters long"
            !PLAYER_NAME_REGEX2.matches(name) ->
                "EVE character names cannot start or end with a \"'\" or \"-\""
            !PLAYER_NAME_REGEX3.matches(name) ->
                "EVE character names can have a first name of up to 25 characters, middle name up to 35 characters, and last name up to 12 characters"
            else -> "Invalid character name"
        }
    }

    companion object {
        private val PLAYER_NAME_REGEX = """[A-z0-9 '-]{3,37}""".toRegex()
        private val PLAYER_NAME_REGEX2 = """[^ '-][A-z0-9 '-]+[^ '-]""".toRegex()
        private val PLAYER_NAME_REGEX3 = """^(?<first>[A-z0-9'-]{1,25})(?<middle> [A-z0-9'-]{1,35})?(?<last> [A-z0-9'-]{1,12})?$""".toRegex()

        // PLAYER_NAME_REGEX, separated into two for reason giving
        private val PLAYER_NAME_REASON_CHARACTERS_REGEX = """[A-z0-9 '-]*""".toRegex()
        private val PLAYER_NAME_REASON_LENGTH_REGEX = """.{3,37}""".toRegex()
    }
}
