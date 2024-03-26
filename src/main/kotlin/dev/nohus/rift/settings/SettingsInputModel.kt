package dev.nohus.rift.settings

sealed interface SettingsInputModel {
    data object Normal : SettingsInputModel
    data object EveInstallation : SettingsInputModel
    data object IntelChannels : SettingsInputModel
}
