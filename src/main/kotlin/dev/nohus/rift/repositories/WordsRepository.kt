package dev.nohus.rift.repositories

import dev.nohus.rift.generated.resources.Res
import kotlinx.coroutines.runBlocking
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.koin.core.annotation.Single

@Single
class WordsRepository {

    @OptIn(ExperimentalResourceApi::class)
    private val words: Set<String> = runBlocking {
        String(Res.readBytes("files/words")).lines().toSet() + setOf("pls", "lol", "blops", "ansi")
    }

    fun isWord(text: String) = text.lowercase() in words
}
