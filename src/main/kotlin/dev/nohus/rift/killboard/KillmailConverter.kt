package dev.nohus.rift.killboard

import dev.nohus.rift.killboard.Killboard.EveKill
import dev.nohus.rift.killboard.Killboard.Zkillboard
import dev.nohus.rift.network.killboard.EveKillKillmail
import dev.nohus.rift.network.killboard.ZkillboardKillmail
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
                corporationId = victim.corporationId,
                allianceId = victim.allianceId,
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
                corporationId = victim?.corporationId?.takeIf { it > 0 },
                allianceId = victim?.allianceId?.takeIf { it > 0 },
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
