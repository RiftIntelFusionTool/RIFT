package dev.nohus.rift.jabber

sealed interface JabberInputModel {
    data object None : JabberInputModel
}
