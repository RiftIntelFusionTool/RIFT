package dev.nohus.rift.jabber.client

import dev.nohus.rift.jabber.JabberAccountRepository
import dev.nohus.rift.jabber.JabberAccountRepository.JabberAccountResult.JabberAccount
import org.koin.core.annotation.Single

/**
 * If there is a Jabber account, connects to it
 */
@Single
class StartJabberUseCase(
    private val jabberAccountRepository: JabberAccountRepository,
    private val jabberClient: JabberClient,
) {

    suspend operator fun invoke(): JabberClient.LoginResult? {
        val account = jabberAccountRepository.getAccount() as? JabberAccount
        return if (account != null) {
            jabberClient.login(account.jid, account.password)
        } else {
            null
        }
    }
}
