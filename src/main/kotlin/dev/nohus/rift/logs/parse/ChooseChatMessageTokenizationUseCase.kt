package dev.nohus.rift.logs.parse

import dev.nohus.rift.logs.parse.ChatMessageParser.Token
import dev.nohus.rift.logs.parse.ChatMessageParser.TokenType
import io.github.oshai.kotlinlogging.KotlinLogging
import org.koin.core.annotation.Single

private val logger = KotlinLogging.logger {}

@Single
class ChooseChatMessageTokenizationUseCase {

    /**
     * Choose the best chat message tokenization out of all provided parsings
     */
    operator fun invoke(tokenizations: Set<List<Token>>): List<Token> {
        var remaining = tokenizations.toList()

        // Take those with the maximum number of killmails
        val groupByKillmails = remaining.groupBy { tokens ->
            tokens.count { it.types.any { it is TokenType.Kill } }
        }
        val maxKillmails = groupByKillmails.keys.max()
        remaining = groupByKillmails[maxKillmails]!!

        // Take those with the maximum number of recognized links
        val groupByKnownLinks = remaining.groupBy { tokens ->
            tokens.count { TokenType.Link in it.types && it.types.size > 1 }
        }
        val maxKnownLinks = groupByKnownLinks.keys.max()
        remaining = groupByKnownLinks[maxKnownLinks]!!

        // Prefer for recognized tokens to cover more text
        val groupBySumOfTokensLength = remaining.groupBy { tokens ->
            tokens.filter { it.types.any { it !is TokenType.Link && it !is TokenType.Question } }.sumOf { it.words.joinToString(" ").length }
        }
        remaining = groupBySumOfTokensLength[groupBySumOfTokensLength.keys.max()]!!

        // Prefer for ship names to cover more text (Exequror Navy Issue rather than Exequror Navy)
        val groupBySumOfShipNamesLength = remaining.groupBy { tokens ->
            tokens.filter { it.types.any { it is TokenType.Ship } }.sumOf { it.words.joinToString(" ").length }
        }
        remaining = groupBySumOfShipNamesLength[groupBySumOfShipNamesLength.keys.max()]!!

        // Prefer last token bo the recognized as something
        if (remaining.any { tokens -> tokens.last().types.isNotEmpty() }) {
            remaining = remaining.filter { tokens -> tokens.last().types.isNotEmpty() }
        }

        // Take those with the maximum number of keywords
        val groupByKeywords = remaining.groupBy { tokens ->
            tokens.count { it.types.any { it is TokenType.Keyword } }
        }
        remaining = groupByKeywords[groupByKeywords.keys.max()]!!

        // Prefer all character names to be recognized
        val characterNames = remaining.flatMap { tokens ->
            tokens.filter { it.types.any { it is TokenType.Player } }.map { it.words }
        }.toSet()
        remaining.filterNot { tokens ->
            tokens.filter { it.types.none { it is TokenType.Player } }.any { it.words in characterNames }
        }.let { if (it.isNotEmpty()) remaining = it }

        // Prefer for a Count to exist before a Ship
        if (remaining.any { tokens ->
                tokens.windowed(2).any { (count, ship) ->
                    count.types.any { it is TokenType.Count } &&
                        ship.types.any { it is TokenType.Ship }
                }
            }
        ) {
            remaining = remaining.filter { tokens ->
                tokens.windowed(2).any { (count, ship) ->
                    count.types.any { it is TokenType.Count } &&
                        ship.types.any { it is TokenType.Ship }
                }
            }
        }

        // Prefer a single plus count
        remaining.filter { tokens ->
            tokens.count { it.types.any { it is TokenType.Count && it.isPlus } } == 1
        }.let { if (it.isNotEmpty()) remaining = it }

        // Prefer for a token to not be absorbed by a question
        val recognizedTokens = remaining.flatMap { tokens ->
            tokens.filter { it.types.isNotEmpty() }.map { it.words.joinToString(" ") }
        }.toSet()
        remaining.filter { tokens ->
            val questions = tokens.filter { it.types.any { it is TokenType.Question } }
            questions.none {
                val text = it.words.joinToString(" ")
                val questionToken = it.types.filterIsInstance<TokenType.Question>().first()
                val beforeQuestion = text.substringBefore(questionToken.questionText)
                val afterQuestion = text.substringAfter(questionToken.questionText)
                recognizedTokens.any { recognizedToken -> recognizedToken in beforeQuestion || recognizedToken in afterQuestion }
            }
        }.let { if (it.isNotEmpty()) remaining = it }

        // Prefer for a token to be recognized if it wouldn't be anything else anyway
        remaining.filter { tokens ->
            val unrecognizedTokens = tokens.filter { it.types.isEmpty() }
            unrecognizedTokens.none {
                val text = it.words.joinToString(" ")
                recognizedTokens.any { recognizedToken -> recognizedToken in text }
            }
        }.let { if (it.isNotEmpty()) remaining = it }

        // Prefer for player names to cover more text (longer player names)
        val groupBySumOfNamesLength = remaining.groupBy { tokens ->
            tokens.filter { it.types.any { it is TokenType.Player } }.sumOf { it.words.joinToString(" ").length }
        }
        remaining = groupBySumOfNamesLength[groupBySumOfNamesLength.keys.max()]!!

        // Prefer for questions to cover more text ("status pls" rather than "status")
        val groupBySumOfQuestionsLength = remaining.groupBy { tokens ->
            tokens.filter { it.types.any { it is TokenType.Question } }.sumOf { it.words.joinToString(" ").length }
        }
        remaining = groupBySumOfQuestionsLength[groupBySumOfQuestionsLength.keys.max()]!!

        // Prefer for a system to be linked
        remaining.filter { tokens ->
            tokens.any { it.types.any { it is TokenType.System } }
        }.let { if (it.isNotEmpty()) remaining = it }

        // Prefer more recognized tokens
        val groupByCountOfRecognizedTokens = remaining.groupBy { tokens ->
            tokens.count { it.types.isNotEmpty() }
        }
        remaining = groupByCountOfRecognizedTokens[groupByCountOfRecognizedTokens.keys.max()]!!

        if (logger.isDebugEnabled()) {
            if (remaining.size > 1) {
                logger.error { "\n" + remaining.joinToString("\n") }
            }
        }

        return remaining.first()
    }
}
