package dev.nohus.rift.logs.parse

import dev.nohus.rift.logs.parse.ChatMessageParser.QuestionType.Location
import dev.nohus.rift.logs.parse.ChatMessageParser.QuestionType.Number
import dev.nohus.rift.logs.parse.ChatMessageParser.QuestionType.ShipTypes
import dev.nohus.rift.logs.parse.ChatMessageParser.QuestionType.Status
import dev.nohus.rift.logs.parse.ChatMessageParser.TokenType.Count
import dev.nohus.rift.logs.parse.ChatMessageParser.TokenType.Gate
import dev.nohus.rift.logs.parse.ChatMessageParser.TokenType.Keyword
import dev.nohus.rift.logs.parse.ChatMessageParser.TokenType.Kill
import dev.nohus.rift.logs.parse.ChatMessageParser.TokenType.Link
import dev.nohus.rift.logs.parse.ChatMessageParser.TokenType.Movement
import dev.nohus.rift.logs.parse.ChatMessageParser.TokenType.Player
import dev.nohus.rift.logs.parse.ChatMessageParser.TokenType.Question
import dev.nohus.rift.logs.parse.ChatMessageParser.TokenType.Ship
import dev.nohus.rift.logs.parse.ChatMessageParser.TokenType.System
import dev.nohus.rift.logs.parse.ChatMessageParser.TokenType.Url
import dev.nohus.rift.repositories.CharactersRepository
import dev.nohus.rift.repositories.CharactersRepository.CharacterState
import dev.nohus.rift.repositories.ShipTypesRepository
import dev.nohus.rift.repositories.SolarSystemsRepository
import dev.nohus.rift.repositories.WordsRepository
import kotlinx.coroutines.coroutineScope
import org.koin.core.annotation.Single
import java.util.LinkedHashSet

@Single
class ChatMessageParser(
    private val solarSystemsRepository: SolarSystemsRepository,
    private val shipTypesRepository: ShipTypesRepository,
    private val charactersRepository: CharactersRepository,
    private val wordsRepository: WordsRepository,
    private val characterNameValidator: CharacterNameValidator,
) {

    sealed interface TokenType {
        data class System(
            val name: String,
        ) : TokenType
        data class Player(
            val characterId: Int,
        ) : TokenType
        data class Ship(
            val name: String,
            val count: Int = 1,
            val isPlural: Boolean = false,
        ) : TokenType
        data object Link : TokenType
        data class Keyword(
            val type: KeywordType,
        ) : TokenType
        data class Count(
            val count: Int,
            val isPlus: Boolean = false,
            val isEquals: Boolean = false,
        ) : TokenType
        data class Question(
            val type: QuestionType,
            val questionText: String,
        ) : TokenType
        data class Kill(
            val name: String,
            val characterId: Int?,
            val target: String,
        ) : TokenType
        data object Url : TokenType
        data class Gate(
            val system: String,
            val isAnsiblex: Boolean = false,
        ) : TokenType
        data class Movement(
            val verb: String,
            val toSystem: String,
            val isGate: Boolean,
        ) : TokenType
    }

    enum class KeywordType {
        NoVisual, Clear, Wormhole, Spike, Ess, GateCamp, CombatProbes, Bubbles
    }

    enum class QuestionType {
        Location, ShipTypes, Number, Status
    }

    data class Token(
        val words: List<String>,
        val types: List<TokenType>,
    ) {
        override fun toString(): String {
            return if (types.isNotEmpty()) {
                "\"${words.joinToString(" ")}\" $types"
            } else {
                "\"${words.joinToString(" ")}\""
            }
        }
    }

    data class Parsing(
        val tokens: List<Token>,
        val remainingWords: List<String>,
    )

    companion object {
        private const val MAX_TOKEN_WORDS = 3
        private const val MAX_INCOMPLETE_TOKENIZATIONS = 1000
        private val SHIP_COUNT_NEXT_REGEX = """[1-9]x|[1-9]\*""".toRegex()
        private val SHIP_COUNT_PREV_REGEX = """x[1-9]""".toRegex()
        private val COUNT_PLUS_REGEX = """\+[1-9][0-9]?|[1-9][0-9]?\+|\+ [1-9][0-9]?|[1-9][0-9]? \+""".toRegex()
        private val COUNT_EQUALS_REGEX = """=[1-9][0-9]?|[1-9][0-9]? neuts""".toRegex()
        private val KILL_MAIL_TARGET_REGEX = """\([A-z ]+\)""".toRegex()
        private val URL_REGEX = """https?://.*""".toRegex()
        private val keywords = mapOf(
            "nv" to KeywordType.NoVisual,
            "clr" to KeywordType.Clear,
            "clear" to KeywordType.Clear,
            "wh" to KeywordType.Wormhole,
            "wormhole" to KeywordType.Wormhole,
            "k162" to KeywordType.Wormhole,
            "spike" to KeywordType.Spike,
            "local spike" to KeywordType.Spike,
            "ess" to KeywordType.Ess,
            "ess intrusion" to KeywordType.Ess,
            "ess hacking" to KeywordType.Ess,
            "gate camp" to KeywordType.GateCamp,
            "gate camping" to KeywordType.GateCamp,
            "gate camped" to KeywordType.GateCamp,
            "combat probes" to KeywordType.CombatProbes,
            "combat scanners" to KeywordType.CombatProbes,
            "bubble" to KeywordType.Bubbles,
            "bubbles" to KeywordType.Bubbles,
            "bubbled" to KeywordType.Bubbles,
        )
        // TODO: "core sister probes"

        // TODO: Character names that maybe should be keywords or ignored: "Gang", "was", "from", "kicked", "Ansiblex", "ansi", "on the"
        // TODO: "WORMHOLE large", "WORMHOLE XL", "WORMHOLE -> SYSTEM"
        private val questions = mapOf(
            "where is he" to Location,
            "where is he?" to Location,
            "loc?" to Location,
            "loc ?" to Location,
            "location?" to Location,
            "location ?" to Location,
            "shiptypes?" to ShipTypes,
            "shiptypes ?" to ShipTypes,
            "shiptype?" to ShipTypes,
            "shiptype ?" to ShipTypes,
            "ship types?" to ShipTypes,
            "ship types ?" to ShipTypes,
            "ship type?" to ShipTypes,
            "ship type ?" to ShipTypes,
            "ships?" to ShipTypes,
            "ships ?" to ShipTypes,
            "ship?" to ShipTypes,
            "ship ?" to ShipTypes,
            "what ships" to ShipTypes,
            "what ships?" to ShipTypes,
            "how many" to Number,
            "how many?" to Number,
            "status?" to Status,
            "status ?" to Status,
            "status please" to Status,
            "status pls" to Status,
            "status" to Status,
            "sts?" to Status,
            "clr?" to Status,
        )
        private val countQualifiers = mapOf(
            "one" to 1,
            "two" to 2,
            "three" to 3,
            "both" to 2,
        )
    }

    /**
     * @param message Chat message to parse
     * @param regionHint Region the intel channel this message is from is for
     * @return All possible tokenizations of the message
     */
    suspend fun parse(
        message: String,
        regionHint: String,
    ): Set<List<Token>> = coroutineScope {
        var replaced = message
            .replace("* ", "  ") // Links sometimes end with *. Replace with a space, so they get detected as links.
            .replace("*)", ")") // Links sometimes end with *. Remove when in parentheses.
            .replace(", ", " ") // Remove commas
            .replace(" ,", " ")
            .replace(",", " ")
            .replace("с", "c") // Replace cyrillic c with latin c
        if (replaced.endsWith("*") && !replaced.matches(""" [0-9]*""".toRegex())) replaced = replaced.dropLast(1) + " "
        if (replaced.startsWith(" ")) replaced = replaced.dropWhile { it == ' ' }
        val collapsed = collapseMultipleSpaces(replaced)
        val words = collapsed.split(" ")
        val completeParsings = mutableListOf<List<Token>>()
        val incompleteParsings = LinkedHashSet<Parsing>()
        incompleteParsings += Parsing(tokens = emptyList(), remainingWords = words)

        val possibleCharacterNames = getPossibleCharacterNames(words)
        val characterNamesStatus = charactersRepository.getCharacterNamesStatus(possibleCharacterNames)

        while (incompleteParsings.isNotEmpty()) {
            if (incompleteParsings.size > MAX_INCOMPLETE_TOKENIZATIONS) {
                // This text has an unusual number of branching tokenizations, do not continue parsing
                return@coroutineScope setOf(listOf(Token(words, types = listOf())))
            }

            val parsing = incompleteParsings.first()
            incompleteParsings.remove(parsing)

            // A token cannot start with a space, the previous token is a link
            if (parsing.remainingWords.first().isBlank() && parsing.tokens.isNotEmpty()) {
                val lastToken = parsing.tokens.last()
                val newTokens = parsing.tokens.dropLast(1) + lastToken.copy(types = lastToken.types.filterNot { it is Link } + Link)

                val remainingWords = parsing.remainingWords.drop(1)
                if (remainingWords.isEmpty()) {
                    completeParsings += newTokens
                } else {
                    incompleteParsings += Parsing(tokens = newTokens, remainingWords = remainingWords)
                }
                continue
            }

            for (wordsToConsume in 1..parsing.remainingWords.size.coerceAtMost(MAX_TOKEN_WORDS)) {
                val token = parsing.remainingWords.take(wordsToConsume)

                // A token cannot contain a space except at the end
                if (token.any { it.isBlank() }) break

                val types = getPossibleTokenTypes(token, characterNamesStatus, regionHint)
                val newToken = Token(token, types = types)
                var tokens = parsing.tokens + newToken
                val isAtEnd = wordsToConsume == parsing.remainingWords.size

                tokens = findKillMail(tokens, characterNamesStatus)
                tokens = findGates(tokens)
                tokens = findMovement(tokens, isAtEnd)
                tokens = findShipCounts(tokens)
                tokens = findQuestions(tokens)
                tokens = findMergeablePlainText(tokens)

                if (isAtEnd) {
                    completeParsings += tokens
                } else {
                    val remainingWords = parsing.remainingWords.drop(wordsToConsume)
                    incompleteParsings += Parsing(tokens = tokens, remainingWords = remainingWords)
                }
            }
        }

        completeParsings
            .asSequence()
            .map(::filterCharactersUntilDone)
            .map { filterMultiTypes(it, regionHint) }
            .map(::mergePlainTextTokens)
            .toSet()
    }

    private fun getPossibleCharacterNames(words: List<String>): List<String> {
        return words.indices
            .asSequence()
            .map { words.drop(it) }
            .flatMap { listOf(it.take(1), it.take(2), it.take(3)).map { it.joinToString(" ") } }
            .filterNot { it.startsWith(" ") || it.endsWith(" ") || it.contains("  ") }
            .filter(characterNameValidator::isValid)
            .toSet()
            .toList()
    }

    private fun collapseMultipleSpaces(message: String): String {
        var squashedMessage = message
        while ("   " in squashedMessage) {
            squashedMessage = squashedMessage.replace("   ", "  ")
        }
        return squashedMessage
    }

    private fun findKillMail(tokens: List<Token>, characterNamesStatus: Map<String, CharacterState>): List<Token> {
        return if (tokens.size >= 3) {
            val threeTokens = tokens.takeLast(3)
            val (t1, t2, t3) = threeTokens
            val killTranslations = listOf("Kill:", "Abschuss:")
            if (t1.words.singleOrNull() in killTranslations && t3.words.joinToString(" ").matches(KILL_MAIL_TARGET_REGEX)) {
                val player = t2.words.joinToString(" ")
                val target = t3.words.joinToString(" ").removePrefix("(").removeSuffix(")")
                val words = threeTokens.flatMap { it.words }
                val characterId = (characterNamesStatus[player] as? CharacterState.Exists)?.characterId
                tokens.dropLast(3) + Token(words, types = listOf(Kill(player, characterId, target)))
            } else {
                tokens
            }
        } else {
            tokens
        }
    }

    private fun findGates(tokens: List<Token>): List<Token> {
        if (tokens.size >= 2) {
            val lastTokens = tokens.takeLast(2)
            val system = lastTokens.mapNotNull { it.types.filterIsInstance<System>().firstOrNull() }.singleOrNull()?.name
            val other = lastTokens.singleOrNull { it.types.none { it is System || it is Keyword } }?.words?.singleOrNull()?.lowercase()
            if (system != null && other != null) {
                val isGate = other == "gate"
                val isAnsiblex = other in listOf("ansiblex", "ansi")
                if (isGate || isAnsiblex) {
                    return tokens.dropLast(2) + Token(lastTokens.flatMap { it.words }, types = listOf(Gate(system, isAnsiblex)))
                }
            }
        }
        return tokens
    }

    private fun findMovement(tokens: List<Token>, isAtEnd: Boolean): List<Token> {
        val keywords = listOf("going", "jumped", "jumping")
        if (tokens.size >= 2) {
            val lastTokens = tokens.takeLast(2)
            if (lastTokens[1].types.filterIsInstance<Gate>().isNotEmpty()) {
                val gate = lastTokens[1].types.filterIsInstance<Gate>().first()
                val before = lastTokens[0].words.joinToString(" ")
                if (before.lowercase() in keywords) {
                    return tokens.dropLast(2) + Token(lastTokens.flatMap { it.words }, types = listOf(Movement(before, gate.system, isGate = true)))
                }
            }
        }
        if (isAtEnd) { // Only consume the last 2 tokens as a movement if they are the end otherwise the next token could've been a gate instead
            if (tokens.size >= 2) {
                val lastTokens = tokens.takeLast(2)
                if (lastTokens[1].types.filterIsInstance<System>().isNotEmpty()) {
                    val system = lastTokens[1].types.filterIsInstance<System>().first().name
                    val before = lastTokens[0].words.joinToString(" ")
                    if (before.lowercase() in keywords) {
                        return tokens.dropLast(2) + Token(lastTokens.flatMap { it.words }, types = listOf(Movement(before, system, isGate = false)))
                    }
                }
            }
        } else {
            if (tokens.size >= 3) {
                val lastTokens = tokens.takeLast(3)
                if (lastTokens[1].types.filterIsInstance<System>().isNotEmpty()) {
                    val system = lastTokens[1].types.filterIsInstance<System>().first().name
                    val before = lastTokens[0].words.joinToString(" ")
                    if (before.lowercase() in keywords) {
                        return tokens.dropLast(3) + Token(lastTokens.flatMap { it.words }, types = listOf(Movement(before, system, isGate = false))) + lastTokens.last()
                    }
                }
            }
        }
        return tokens
    }

    private fun findShipCounts(tokens: List<Token>): List<Token> {
        if (tokens.size >= 2) {
            run {
                val (count, ship) = tokens.takeLast(2)
                val isNextTokenShip = ship.types.any { it is Ship }
                val word = count.words.singleOrNull()
                if (isNextTokenShip && word != null) {
                    val number = when {
                        word.toIntOrNull()?.let { it < 10 } == true -> {
                            word.toInt() // "2 loki"
                        }
                        word in countQualifiers.keys -> {
                            countQualifiers.getValue(word) // "two loki"
                        }
                        word.matches(SHIP_COUNT_NEXT_REGEX) -> {
                            word.filter { it.isDigit() }.toInt() // "2x loki", "2* loki"
                        }
                        else -> null
                    }
                    if (number != null) {
                        val shipWithCount = ship.copy(
                            words = count.words + ship.words,
                            types = ship.types.map { type -> if (type is Ship) type.copy(count = number) else type },
                        )
                        return tokens.dropLast(2) + shipWithCount
                    }
                }
            }
            run {
                val (ship, count) = tokens.takeLast(2)
                val isPrevTokenShip = ship.types.any { it is Ship }
                val word = count.words.singleOrNull()
                if (isPrevTokenShip && word != null) {
                    val number = if (word.matches(SHIP_COUNT_PREV_REGEX)) {
                        word.filter { it.isDigit() }.toInt() // "loki x2"
                    } else {
                        null
                    }
                    if (number != null) {
                        val shipWithCount = ship.copy(
                            words = ship.words + count.words,
                            types = ship.types.map { type -> if (type is Ship) type.copy(count = number) else type },
                        )
                        return tokens.dropLast(2) + shipWithCount
                    }
                }
            }
        }
        val token = tokens.last()
        val text = token.words.joinToString(" ")
        return if (text.matches(SHIP_COUNT_PREV_REGEX) || text.matches(SHIP_COUNT_NEXT_REGEX)) {
            val count = text.filter { it.isDigit() }.toInt()
            tokens.dropLast(1) + token.copy(types = listOf(Count(count))) // "x2"
        } else if (text.matches(COUNT_PLUS_REGEX)) {
            val count = text.filter { it.isDigit() }.toInt()
            tokens.dropLast(1) + token.copy(types = listOf(Count(count, isPlus = true))) // "+2", "2+", "+ 2", "2 +"
        } else if (text.lowercase().matches(COUNT_EQUALS_REGEX)) {
            val count = text.filter { it.isDigit() }.toInt()
            tokens.dropLast(1) + token.copy(types = listOf(Count(count, isEquals = true))) // "=15"
        } else {
            tokens
        }
    }

    private fun findQuestions(tokens: List<Token>): List<Token> {
        val token = tokens.last()
        val originalText = token.words.joinToString(" ")
        val text = originalText.lowercase()
        val question = questions.entries.find { (question, _) -> question == text }
        return if (question != null) {
            val originalQuestionTextIndex = text.indexOf(question.key)
            val originalQuestionText = originalText.substring(originalQuestionTextIndex, originalQuestionTextIndex + question.key.length)
            tokens.dropLast(1) + token.copy(types = listOf(Question(type = question.value, questionText = originalQuestionText)))
        } else {
            tokens
        }
    }

    private fun findMergeablePlainText(tokens: List<Token>): List<Token> {
        return if (tokens.size >= 4) {
            // Last two are potentially going to be used for a 3-token find (e.g. killmail)
            mergePlainTextTokens(tokens.dropLast(2)) + tokens.takeLast(2)
        } else {
            tokens
        }
    }

    private fun filterCharactersUntilDone(parsing: List<Token>): List<Token> {
        var previous: List<Token>
        var new = parsing
        do {
            previous = new
            new = filterCharacters(previous)
        } while (new != previous)
        return new
    }

    private fun filterCharacters(parsing: List<Token>): List<Token> {
        return buildList {
            for ((index, token) in parsing.withIndex()) {
                if (token.types.any { it is Player }) {
                    val isEnglish = token.words.all { wordsRepository.isWord(it) }
                    val isLowercase = token.words.all { word -> word.all { it.isLowerCase() } }
                    if (isEnglish) {
                        if (isLowercase) {
                            val before = parsing.getOrNull(index - 1)?.takeIf { it.types.all { it is Question || it is Link } }?.words?.lastOrNull()
                            val after = parsing.getOrNull(index + 1)?.takeIf { it.types.all { it is Question || it is Link } }?.words?.firstOrNull()
                            val surroundingLowercaseWords = listOfNotNull(before, after)
                                .filter { word ->
                                    word.all { it.isLowerCase() || it in listOf('\'', '?') } || word in listOf("I")
                                }
                            if (surroundingLowercaseWords.isNotEmpty()) {
                                // Lowercase English words character name touches a lowercase plaintext, ignore
                                add(token.copy(types = token.types.filterNot { it is Player }))
                                continue
                            }
                        } else {
                            val isOnlyFirstLetterUppercase = token.words.joinToString("").drop(1).all { it.isLowerCase() }
                            if (isOnlyFirstLetterUppercase) {
                                val nextWord = parsing.getOrNull(index + 1)?.takeIf { it.types.all { it is Question || it is Link } }?.words?.firstOrNull()
                                val nextLowercaseWord = nextWord?.all { it.isLowerCase() || it in listOf('\'', '?') }
                                if (nextLowercaseWord == true) {
                                    // English words character name with first capital letter, with next word being lowercase plaintext, ignore
                                    add(token.copy(types = token.types.filterNot { it is Player }))
                                    continue
                                }
                            }
                        }
                    }
                }
                add(token)
            }
        }
    }

    private fun filterMultiTypes(parsing: List<Token>, regionHint: String): List<Token> {
        // TODO: We could expose tokens with a single type instead of list of types after this
        return buildList {
            for (token in parsing) {
                if (token.types.any { it is Ship } && token.types.any { it is System }) {
                    // Token is both a system and a ship (Naga)
                    val otherTokens = parsing - token
                    val hasOtherSystem = otherTokens.any { it.types.any { it is System } }
                    if (hasOtherSystem) {
                        add(token.copy(types = token.types.filterIsInstance<Ship>()))
                    } else {
                        add(token.copy(types = token.types.filterIsInstance<System>()))
                    }
                    continue
                }
                if (token.types.any { it is System } && token.types.any { it is Player }) {
                    // Token is both a system and a player
                    val inRegionSystem = token.types.filterIsInstance<System>()
                        .firstOrNull { solarSystemsRepository.getRegionBySystem(it.name) == regionHint }
                    if (inRegionSystem != null) {
                        // If the system is in this region, choose the system
                        add(token.copy(types = listOf(inRegionSystem)))
                    } else {
                        // Otherwise choose the character name
                        add(token.copy(types = token.types.filterIsInstance<Player>()))
                    }
                    continue
                }
                if (token.types.singleOrNull() == Link) {
                    add(token.copy(types = emptyList()))
                    continue
                }
                add(token)
            }
        }
    }

    private fun mergePlainTextTokens(parsing: List<Token>): List<Token> {
        return buildList {
            for (token in parsing) {
                val new = if (token.types.isEmpty() && lastOrNull()?.types?.isEmpty() == true) {
                    val merged = Token(last().words + token.words, types = emptyList())
                    removeLast()
                    merged
                } else {
                    token
                }
                add(new)
            }
        }
    }

    private fun getPossibleTokenTypes(
        words: List<String>,
        characterNamesStatus: Map<String, CharacterState>,
        regionHint: String,
    ): List<TokenType> {
        val text = words.joinToString(" ")
        return buildList {
            if (words.singleOrNull()?.matches(URL_REGEX) == true) {
                add(Url)
                return@buildList
            }

            val systemName = solarSystemsRepository.getSystemName(text, regionHint)
            if (systemName != null) add(System(systemName))

            val shipText = text
                .replace("(", "").replace(")", "").replace(".", "")
            var shipName = shipTypesRepository.getShip(shipText)
            if (shipName != null) {
                add(Ship(shipName, isPlural = false))
            } else if (text.last() == 's') {
                shipName = shipTypesRepository.getShip(text.dropLast(1))
                if (shipName != null) add(Ship(shipName, isPlural = true))
            }

            if (shipName == null) { // Ship names are assumed to be ships
                if (characterNameValidator.isValid(text)) {
                    val status = characterNamesStatus[text]
                    if (status is CharacterState.Exists) add(Player(status.characterId))
                }
            }

            val keywordText = words.joinToString(" ")
                .replace("(", "").replace(")", "").replace(".", "")
                .lowercase()
            val keywordType = keywords[keywordText]
            if (keywordType != null) {
                clear()
                add(Keyword(type = keywordType))
            }
        }
    }
}
