package dev.nohus.rift.sso.authentication

data class SsoException(
    val errorResponse: SsoErrorResponse?,
) : Exception()
