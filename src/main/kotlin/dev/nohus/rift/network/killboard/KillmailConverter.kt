package dev.nohus.rift.network.killboard

import dev.nohus.rift.network.killboard.Killboard.EveKill
import dev.nohus.rift.network.killboard.Killboard.Zkillboard
import org.koin.core.annotation.Single

@Single
class KillmailConverter {

    fun convert(killmail: ZkillboardKillmail): Killmail = with(killmail) {
        return Killmail(
            killboard = Zkillboard,
            killmailId = killmailId,
            killmailTime = killmailTime,
            solarSystemId = solarSystemId,
            url = zkb.url,
            victim = Victim(
                characterId = victim.characterId,
                shipTypeId = victim.shipTypeId,
            ),
            attackers = attackers.map { attacker ->
                Attacker(
                    characterId = attacker.characterId,
                    shipTypeId = attacker.shipTypeId,
                )
            },
        )
    }

    fun convert(killmail: EveKillKillmail): Killmail? = with(killmail) {
        return Killmail(
            killboard = EveKill,
            killmailId = killmailId ?: return null,
            killmailTime = killTimeStr ?: return null,
            solarSystemId = systemId ?: return null,
            url = "https://eve-kill.com/kill/$killmailId",
            victim = Victim(
                characterId = victim?.characterId?.takeIf { it > 0 },
                shipTypeId = victim?.shipId?.takeIf { it > 0 },
            ),
            attackers = attackers.map { attacker ->
                Attacker(
                    characterId = attacker.characterId?.takeIf { it > 0 },
                    shipTypeId = attacker.shipId?.takeIf { it > 0 },
                )
            },
        )
    }
}
