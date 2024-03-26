package dev.nohus.rift.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import dev.nohus.rift.ViewModel
import dev.nohus.rift.di.koin
import org.koin.core.parameter.parametersOf
import java.awt.Desktop
import java.net.URI
import java.net.URISyntaxException

fun URI.openBrowser() {
    try {
        Desktop.getDesktop().browse(this)
    } catch (e: UnsupportedOperationException) {
        Runtime.getRuntime().exec("xdg-open $this")
    }
}

fun String.toURIOrNull(): URI? {
    return try {
        URI(this)
    } catch (e: URISyntaxException) {
        null
    }
}

@Composable
inline fun <reified VM : ViewModel> viewModel(): VM {
    return remember { koin.get() }
}

@Composable
inline fun <reified VM : ViewModel, I> viewModel(inputModel: I): VM {
    return remember { koin.get { parametersOf(inputModel) } }
}
