package dev.nohus.rift.clipboard

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okio.IOException
import org.koin.core.annotation.Single
import java.awt.Toolkit
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.StringSelection
import java.awt.datatransfer.UnsupportedFlavorException

@Single
class Clipboard {

    private val _state = MutableStateFlow<String?>(null)
    val state = _state.asStateFlow()

    suspend fun start() = coroutineScope {
        launch {
            observeClipboard()
        }
    }

    private suspend fun observeClipboard() = withContext(Dispatchers.IO) {
        while (true) {
            val contents = try {
                clipboard.getContents(null)?.getTransferData(DataFlavor.stringFlavor) as? String
            } catch (e: IllegalStateException) {
                null
            } catch (e: UnsupportedFlavorException) {
                null
            } catch (e: IOException) {
                null
            }
            _state.value = contents
            delay(250)
        }
    }

    companion object {
        private val clipboard = Toolkit.getDefaultToolkit().systemClipboard

        fun copy(text: String) {
            val selection = StringSelection(text)
            clipboard.setContents(selection, null)
        }
    }
}
