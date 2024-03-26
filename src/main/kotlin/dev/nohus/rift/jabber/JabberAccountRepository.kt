package dev.nohus.rift.jabber

import dev.nohus.rift.settings.persistence.Settings
import org.koin.core.annotation.Single

@Single
class JabberAccountRepository(
    private val settings: Settings,
) {

    sealed interface JabberAccountResult {
        data object NoAccount : JabberAccountResult
        data class JabberAccount(
            val jid: String,
            val password: String,
        ) : JabberAccountResult
    }

    fun getAccount(): JabberAccountResult {
        val jidLocalPart = settings.jabberJidLocalPart ?: return JabberAccountResult.NoAccount
        val password = settings.jabberPassword ?: return JabberAccountResult.NoAccount
        val jid = "$jidLocalPart@goonfleet.com"
        return JabberAccountResult.JabberAccount(jid, password)
    }

    fun setAccount(jidLocalPart: String, password: String) {
        settings.jabberJidLocalPart = jidLocalPart
        settings.jabberPassword = password
    }

    fun clearAccount() {
        settings.jabberJidLocalPart = null
        settings.jabberPassword = null
    }
}
