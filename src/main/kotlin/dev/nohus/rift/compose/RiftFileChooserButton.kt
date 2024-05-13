package dev.nohus.rift.compose

import androidx.compose.runtime.Composable
import dev.nohus.rift.windowing.LocalRiftWindow
import java.nio.file.Path
import javax.swing.JFileChooser
import javax.swing.UIManager
import javax.swing.filechooser.FileNameExtensionFilter

@Composable
fun RiftFileChooserButton(
    text: String = "Select",
    fileSelectionMode: Int = JFileChooser.FILES_ONLY,
    typesDescription: String,
    extensions: List<String> = emptyList(),
    currentPath: String? = null,
    type: ButtonType = ButtonType.Primary,
    cornerCut: ButtonCornerCut = ButtonCornerCut.BottomRight,
    onFileChosen: (Path) -> Unit,
) {
    val frame = LocalRiftWindow.current ?: return
    RiftButton(
        text = text,
        type = type,
        cornerCut = cornerCut,
        onClick = {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName())
            val chooser = JFileChooser(currentPath)
            chooser.fileSelectionMode = fileSelectionMode
            if (extensions.isNotEmpty()) {
                chooser.fileFilter = FileNameExtensionFilter(typesDescription, *extensions.toTypedArray())
            }
            val returnValue = chooser.showOpenDialog(frame)
            if (returnValue == JFileChooser.APPROVE_OPTION) {
                onFileChosen(chooser.selectedFile.toPath())
            }
        },
    )
}
