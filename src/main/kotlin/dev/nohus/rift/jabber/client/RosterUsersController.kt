package dev.nohus.rift.jabber.client

import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.jivesoftware.smack.SmackException
import org.jivesoftware.smack.XMPPConnection
import org.jivesoftware.smack.XMPPException
import org.jivesoftware.smack.packet.Presence
import org.jivesoftware.smack.roster.Roster
import org.jivesoftware.smack.roster.RosterEntry
import org.jivesoftware.smack.roster.RosterListener
import org.jivesoftware.smack.roster.RosterLoadedListener
import org.jivesoftware.smackx.vcardtemp.VCardManager
import org.jxmpp.jid.BareJid
import org.jxmpp.jid.Jid
import org.jxmpp.jid.impl.JidCreate
import org.jxmpp.stringprep.XmppStringprepException
import org.koin.core.annotation.Factory
import java.util.concurrent.Executors

private val logger = KotlinLogging.logger {}

@Factory
class RosterUsersController {

    private val _state = MutableStateFlow<Map<BareJid, RosterUser>>(emptyMap())
    val state = _state.asStateFlow()

    private val dispatcher = Executors.newCachedThreadPool().asCoroutineDispatcher()
    private val scope = CoroutineScope(Job() + dispatcher)
    private var roster: Roster? = null

    data class RosterUser(
        val entry: RosterEntry,
        val jid: BareJid,
        val name: String,
        val groups: List<String>,
        val bestPresence: RosterUserPresence,
        val presences: List<RosterUserPresence>,
        val vCard: RosterUserVCard?,
        val isSubscriptionPending: Boolean,
    )

    data class RosterUserPresence(
        val jid: Jid,
        val mode: PresenceMode,
        val status: String?,
    )

    data class RosterUserVCard(
        val nickname: String?,
        val avatarMime: String?,
        val avatarBytes: ByteArray?,
    )

    enum class PresenceMode {
        FreeToChat, Available, Away, ExtendedAway, DoNotDisturb
    }

    fun initialize(connection: XMPPConnection) {
        val roster = Roster.getInstanceFor(connection)
        this.roster = roster
        val vCardManager = VCardManager.getInstanceFor(connection)
        roster.addRosterLoadedListener(object : RosterLoadedListener {
            override fun onRosterLoaded(roster: Roster) {
                updateAllRosterUsers(roster)
                roster.addRosterListener(object : RosterListener {
                    override fun entriesAdded(addresses: Collection<Jid>) {
                        onRosterEntriesUpdated(roster, vCardManager, addresses)
                    }

                    override fun entriesUpdated(addresses: Collection<Jid>) {
                        onRosterEntriesUpdated(roster, vCardManager, addresses)
                    }

                    override fun entriesDeleted(addresses: Collection<Jid>) {
                        onRosterEntriesDeleted(addresses)
                    }

                    override fun presenceChanged(presence: Presence) {
                        onRosterEntriesUpdated(roster, vCardManager, listOf(presence.from))
                    }
                })
            }

            override fun onRosterLoadingFailed(exception: java.lang.Exception) {
                logger.error(exception) { "Roster loading failed" }
            }
        })
    }

    fun onLogout() {
        roster = null
        _state.update { emptyMap() }
    }

    fun addContact(jidLocalPart: String, name: String, groups: List<String>) {
        try {
            val jid = JidCreate.bareFrom("$jidLocalPart@goonfleet.com")
            roster?.createItemAndRequestSubscription(jid, name, groups.filter { it.isNotBlank() }.toTypedArray())
        } catch (e: XMPPException) {
            logger.error { "Could not add contact: $e" }
        } catch (e: XmppStringprepException) {
            logger.error { "Could not add contact, invalid JID: $e" }
        }
    }

    fun removeContact(rosterUser: RosterUser) {
        try {
            roster?.removeEntry(rosterUser.entry)
        } catch (e: XMPPException) {
            logger.error { "Could not remove contact: $e" }
        }
    }

    private fun onRosterEntriesUpdated(roster: Roster, vCardManager: VCardManager, addresses: Collection<Jid>) {
        val users = addresses.mapNotNull { jid ->
            val entry = roster.getEntry(jid.asBareJid()) ?: return@mapNotNull null
            getRosterUser(roster, entry)
        }
        updateRosterUsers(users)
        users.forEach {
            loadVCard(vCardManager, it.jid)
        }
    }

    private fun onRosterEntriesDeleted(addresses: Collection<Jid>) {
        deleteRosterUsers(addresses.map { it.asBareJid() }.toSet())
    }

    private fun updateAllRosterUsers(roster: Roster) {
        val users = roster.entries.map { entry ->
            getRosterUser(roster, entry)
        }
        updateRosterUsers(users)
    }

    private fun updateRosterUsers(users: List<RosterUser>) {
        val existing = _state.value
        val new = existing + users.associateBy { it.jid }
        _state.update { new }
    }

    private fun deleteRosterUsers(addresses: Set<BareJid>) {
        val existing = _state.value
        val new = existing - addresses
        _state.update { new }
    }

    private fun getRosterUser(roster: Roster, entry: RosterEntry): RosterUser {
        val presences = roster.getAllPresences(entry.jid).map(::getRosterPresence)
        return RosterUser(
            entry = entry,
            jid = entry.jid,
            name = entry.name,
            groups = entry.groups.map { it.name },
            bestPresence = getRosterPresence(roster.getPresence(entry.jid)),
            presences = presences,
            vCard = null,
            isSubscriptionPending = entry.isSubscriptionPending,
        )
    }

    private fun getRosterPresence(presence: Presence): RosterUserPresence {
        val mode = when (presence.mode!!) {
            Presence.Mode.chat -> PresenceMode.FreeToChat
            Presence.Mode.available -> PresenceMode.Available
            Presence.Mode.away -> PresenceMode.Away
            Presence.Mode.xa -> PresenceMode.ExtendedAway
            Presence.Mode.dnd -> PresenceMode.DoNotDisturb
        }
        return RosterUserPresence(
            jid = presence.from,
            mode = mode,
            status = presence.status,
        )
    }

    private fun loadVCard(vCardManager: VCardManager, jid: Jid) {
        scope.launch {
            val bareJid = jid.asBareJid()
            val entityBareJid = bareJid.asEntityBareJidIfPossible() ?: return@launch
            try {
                val vCard = vCardManager.loadVCard(entityBareJid).let {
                    RosterUserVCard(
                        nickname = it.nickName,
                        avatarMime = it.avatarMimeType,
                        avatarBytes = it.avatar,
                    )
                }
                val user = _state.value[bareJid]?.copy(vCard = vCard)
                if (user != null) {
                    _state.update { it + (bareJid to user) }
                }
            } catch (e: SmackException) {
                // Could not load
            } catch (e: XMPPException) {
                // Could not load
            } catch (e: InterruptedException) {
                // Could not load
            }
        }
    }
}
