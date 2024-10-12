package dev.nohus.rift.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.withStyle
import dev.nohus.rift.ViewModel
import dev.nohus.rift.di.koin
import org.koin.core.parameter.parametersOf
import java.awt.Desktop
import java.io.IOException
import java.net.URI
import java.net.URISyntaxException
import java.nio.file.Path
import kotlin.io.path.createFile

fun URI.openBrowser() {
    try {
        Desktop.getDesktop().browse(this)
    } catch (e: UnsupportedOperationException) {
        Runtime.getRuntime().exec(arrayOf("xdg-open", toString()))
    }
}

fun Path.openFileManager() {
    try {
        Desktop.getDesktop().open(toFile())
    } catch (e: UnsupportedOperationException) {
        Runtime.getRuntime().exec(arrayOf("xdg-open", toString()))
    }
}

fun Path.createNewFile() {
    try {
        createFile()
    } catch (ignored: IOException) {}
}

fun String.toURIOrNull(): URI? {
    return try {
        URI(this)
    } catch (e: URISyntaxException) {
        null
    }
}

operator fun MatchResult.get(key: String): String {
    return groups[key]!!.value
}

@Composable
inline fun <reified VM : ViewModel> viewModel(): VM {
    return remember { koin.get() }
}

@Composable
inline fun <reified VM : ViewModel, I> viewModel(inputModel: I): VM {
    return remember { koin.get { parametersOf(inputModel) } }
}

fun AnnotatedString.Builder.withColor(color: Color, block: AnnotatedString.Builder.() -> Unit) {
    withStyle(style = SpanStyle(color = color)) {
        block()
    }
}

fun <T> List<T>.toggle(element: T): List<T> {
    return if (element in this) this - element else this + element
}
