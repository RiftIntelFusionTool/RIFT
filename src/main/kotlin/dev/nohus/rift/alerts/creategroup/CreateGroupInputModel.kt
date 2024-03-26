package dev.nohus.rift.alerts.creategroup

sealed interface CreateGroupInputModel {
    data object New : CreateGroupInputModel
    data class Rename(val name: String) : CreateGroupInputModel
}
