package dev.nohus.rift.utils.openwindows

interface GetOpenEveClientsUseCase {
    operator fun invoke(): List<String>?
}
