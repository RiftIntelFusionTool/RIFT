package dev.nohus.rift.utils.openwindows

class MacGetOpenEveClientsUseCase : GetOpenEveClientsUseCase {

    // On macOS there is no way to access window titles of other applications
    override fun invoke(): List<String>? {
        return null
    }
}
