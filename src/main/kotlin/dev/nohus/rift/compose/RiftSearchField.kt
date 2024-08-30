package dev.nohus.rift.compose

import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.dp

@Composable
fun RiftSearchField(
    search: String?,
    isCompact: Boolean,
    onSearchChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var search by remember { mutableStateOf(search ?: "") }
    val focusManager = LocalFocusManager.current
    RiftTextField(
        text = search,
        placeholder = "Search",
        onTextChanged = {
            search = it
            onSearchChange(it)
        },
        height = if (isCompact) 24.dp else 32.dp,
        onDeleteClick = {
            search = ""
            onSearchChange("")
        },
        modifier = modifier
            .width(150.dp)
            .onKeyEvent {
                when (it.key) {
                    Key.Escape -> {
                        focusManager.clearFocus()
                        true
                    }

                    else -> false
                }
            },
    )
}
