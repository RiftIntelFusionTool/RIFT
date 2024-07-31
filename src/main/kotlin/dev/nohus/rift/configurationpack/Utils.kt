package dev.nohus.rift.configurationpack

import dev.nohus.rift.settings.persistence.ConfigurationPack

val ConfigurationPack?.displayName: String get() = when (this) {
    ConfigurationPack.Imperium -> "The Imperium"
    ConfigurationPack.TheInitiative -> "The Initiative."
    null -> "Default"
}
