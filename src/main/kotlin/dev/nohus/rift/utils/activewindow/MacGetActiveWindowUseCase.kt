package dev.nohus.rift.utils.activewindow

class MacGetActiveWindowUseCase : GetActiveWindowUseCase {

    // On macOS there is no way to access window titles of other applications
    override fun invoke(): String? {
        return null
    }
}
