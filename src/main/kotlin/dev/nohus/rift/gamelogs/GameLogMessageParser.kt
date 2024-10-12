package dev.nohus.rift.gamelogs

import dev.nohus.rift.alerts.AlertsTriggerController
import dev.nohus.rift.clones.ClonesRepository
import dev.nohus.rift.gamelogs.GameLogAction.Attacking
import dev.nohus.rift.gamelogs.GameLogAction.BeingWarpScrambled
import dev.nohus.rift.gamelogs.GameLogAction.CloneJumping
import dev.nohus.rift.gamelogs.GameLogAction.Decloaked
import dev.nohus.rift.gamelogs.GameLogAction.UnderAttack
import dev.nohus.rift.logs.parse.GameLogMessageWithMetadata
import org.koin.core.annotation.Single
import java.time.Duration
import java.time.Instant

@Single
class GameLogMessageParser(
    private val alertsTriggerController: AlertsTriggerController,
    private val recentTargetsRepository: RecentTargetsRepository,
    private val clonesRepository: ClonesRepository,
) {

    fun onMessage(messageWithMetadata: GameLogMessageWithMetadata) {
        val type = messageWithMetadata.message.type
        val message = removeFormatting(messageWithMetadata.message.message)
        val action: GameLogAction? = when (type) {
            "combat" -> checkCombatMessage(message)
            "bounty" -> checkBountyMessage(message)
            "notify" -> checkNotifyMessage(message)
            "hint" -> checkHintMessage(message)
            "None" -> checkNoneMessage(message)
            "question" -> checkQuestionMessage(message)
            "info" -> checkInfoMessage(message)
            "warning" -> checkWarningMessage(message)
            "mining" -> checkMiningMessage(message)
            else -> null
        }
        if (action != null) {
            if (Duration.between(messageWithMetadata.message.timestamp, Instant.now()) < Duration.ofSeconds(5)) {
                alertsTriggerController.onNewGameLogAction(action, messageWithMetadata.metadata.characterId)
                if (action is CloneJumping) clonesRepository.onCloneJump()
            }
            recentTargetsRepository.onNewGameLogAction(action)
        }
    }

    private fun checkCombatMessage(message: String): GameLogAction? {
        COMBAT_HIT_REGEX.onMatch(message) { (damage, target, hit) -> return UnderAttack(target) }
        COMBAT_MISS_REGEX.onMatch(message) { (target) -> return UnderAttack(target) }
        COMBAT_ENERGY_NEUTRALIZED_REGEX.onMatch(message) { (damage, target, target2) -> return UnderAttack(target) }
        COMBAT_ATTACK_HIT.onMatch(message) { (damage, target, weapon, hit) -> return Attacking(target) }
        COMBAT_ATTACK_MISS.onMatch(message) { (weapon, target, weapon2) -> return Attacking(target) }
        COMBAT_WARP_SCRAMBLE_ATTEMPT.onMatch(message) { (source, target) -> return if (target == "you") BeingWarpScrambled(source) else null }
        COMBAT_REMOTE_SHIELD_BOOSTED.onMatch(message) { return null }
        COMBAT_REMOTE_ARMOR_REPAIRED.onMatch(message) { return null }
        COMBAT_REMOTE_ARMOR_REPAIRED_TO.onMatch(message) { return null }
        return null
    }

    private fun checkBountyMessage(message: String): GameLogAction? {
        BOUNTY_ADDED.onMatch(message) { (amount) -> return null }
        return null
    }

    private fun checkNotifyMessage(message: String): GameLogAction? {
        NOTIFY_WEAPON_RAN_OUT_OF_CHARGES.onMatch(message) { (weapon) -> return null }
        NOTIFY_WEAPON_DEACTIVATES_TARGET_EXPLODED.onMatch(message) { (weapon, target) -> return null }
        NOTIFY_CLONE_JUMPING.onMatch(message) { return CloneJumping }
        NOTIFY_DISEMBARKING_FROM_SHIP.onMatch(message) { return null }
        NOTIFY_SESSION_CHANGE_ALREADY_IN_PROGRESS.onMatch(message) { return null }
        NOTIFY_DRONES_ENGAGING.onMatch(message) { (target) -> return null }
        NOTIFY_MODULE_DEACTIVATES_TARGET_GONE.onMatch(message) { (module) -> return null }
        NOTIFY_DRONES_FAIL_TOO_FAR.onMatch(message) { (target, range) -> return null }
        NOTIFY_TARGET_TOO_FAR.onMatch(message) { (target, range) -> return null }
        NOTIFY_TARGET_NOT_PRESENT.onMatch(message) { return null }
        NOTIFY_TARGET_TOO_FAR_FOR_MODULE.onMatch(message) { (target, module, range) -> return null }
        NOTIFY_LOADING_CHARGES.onMatch(message) { (charge, module, time) -> return null }
        NOTIFY_SHIP_STOPPING.onMatch(message) { return null }
        NOTIFY_CANNOT_WHILE_WARPING.onMatch(message) { return null }
        NOTIFY_PLEASE_WAIT.onMatch(message) { return null }
        NOTIFY_NOT_ENOUGH_CAPACITOR.onMatch(message) { (module, required, remaining) -> return null }
        NOTIFY_SALVAGING_FAILED.onMatch(message) { return null }
        NOTIFY_SALVAGING_FAILED_ALREADY_DONE.onMatch(message) { (target) -> return null }
        NOTIFY_SALVAGING_SUCCEEDED.onMatch(message) { (target) -> return null }
        NOTIFY_SALVAGING_SUCCEEDED_EMPTY.onMatch(message) { (target) -> return null }
        NOTIFY_MAX_TARGETS.onMatch(message) { (max) -> return null }
        NOTIFY_RECONNECTING_TO_DRONES.onMatch(message) { return null }
        NOTIFY_CARGO_TOO_FAR_APPROACHING.onMatch(message) { return null }
        NOTIFY_CONTAINER_TOO_FAR_TO_PLACE.onMatch(message) { return null }
        NOTIFY_CONTAINER_TOO_FAR_TO_REMOVE.onMatch(message) { return null }
        NOTIFY_ATTEMPTING_TO_ACTIVE_PASSIVE_MODULE.onMatch(message) { return null }
        NOTIFY_CAN_ONLY_GROUP_TURRETS_AND_LAUNCHERS.onMatch(message) { return null }
        NOTIFY_ITEM_NO_LONGER_WITHIN_REACH.onMatch(message) { return null }
        NOTIFY_PI_INVALID_ROUTE.onMatch(message) { return null }
        NOTIFY_CLOAK_CANNOT_ACTIVATE_NEARBY.onMatch(message) { (distance, nearby) -> return null }
        NOTIFY_CLOAK_DEACTIVATED_NEARBY.onMatch(message) { (nearby) -> return Decloaked(nearby) }
        NOTIFY_CLOAKED_CANNOT_TARGET.onMatch(message) { return null }
        NOTIFY_CLOAKED_CANNOT_USE_MODULE.onMatch(message) { return null }
        NOTIFY_WARPING_CANNOT_TARGET.onMatch(message) { (target) -> return null }
        NOTIFY_WARPING_CANNOT_USE_MODULE.onMatch(message) { return null }
        NOTIFY_EXTERNAL_FACTORS_PREVENT_MODULE.onMatch(message) { (module) -> return null }
        NOTIFY_CANNOT_LOAD_WHILE_ACTIVE.onMatch(message) { (module) -> return null }
        NOTIFY_NOT_ENOUGH_CHARGES_TO_LOAD_ALL.onMatch(message) { return null }
        NOTIFY_SCANNER_RECALIBRATING.onMatch(message) { return null }
        NOTIFY_POCO_COOLDOWN.onMatch(message) { (duration) -> return null }
        NOTIFY_CANNOT_WHILE_UNDOCKING.onMatch(message) { (duration) -> return null }
        NOTIFY_POCO_DRAG_FIRST.onMatch(message) { return null }
        NOTIFY_PI_EXPEDITED_TRANSFER_SELECT.onMatch(message) { return null }
        NOTIFY_UNABLE_TO_ONLINE_MODULE_MISSING_SKILLS.onMatch(message) { return null }
        NOTIFY_FOLLOWING_WARP.onMatch(message) { return null }
        NOTIFY_MODULE_DEACTIVATES_RESOURCE_GONE.onMatch(message) { return null }
        NOTIFY_ALL_DRONES_RETURNING.onMatch(message) { return null }
        NOTIFY_REQUESTED_TO_DOCK.onMatch(message) { return null }
        NOTIFY_SETTING_COURSE_TO_DOCK.onMatch(message) { return null }
        NOTIFY_DOCKING_REQUEST_ACCEPTED.onMatch(message) { return null }
        NOTIFY_CANNOT_WHILE_DOCKING.onMatch(message) { return null }
        NOTIFY_DOCKING_ALREADY_IN_PROGRESS.onMatch(message) { return null }
        NOTIFY_CHARACTER_AGGRO_SENTRY_GUNS.onMatch(message) { return null }
        NOTIFY_COMMAND_BURST_BONUSES.onMatch(message) { return null }
        NOTIFY_PI_BUSY.onMatch(message) { return null }
        return null
    }

    private fun checkHintMessage(message: String): GameLogAction? {
        HINT_ATTEMPTING_TO_JOIN_A_CHANNEL.onMatch(message) { return null }
        HINT_MODULE_ALREADY_ACTIVE.onMatch(message) { (module) -> return null }
        return null
    }

    private fun checkNoneMessage(message: String): GameLogAction? {
        NONE_JUMPING.onMatch(message) { (from, to) -> return null }
        NONE_UNDOCKING.onMatch(message) { return null }
        return null
    }

    private fun checkQuestionMessage(message: String): GameLogAction? {
        QUESTION_REMOVE_LOCATION.onMatch(message) { (location) -> return null }
        QUESTION_SELF_DESTRUCT.onMatch(message) { (location) -> return null }
        QUESTION_QUIT_GAME.onMatch(message) { return null }
        QUESTION_ITEMS_REDEEMED.onMatch(message) { (character, items) -> return null }
        QUESTION_EXIT_SIMULATION.onMatch(message) { return null }
        QUESTION_REPACKAGE.onMatch(message) { return null }
        QUESTION_COURIER_CONTRACT_LARGE.onMatch(message) { (size) -> return null }
        QUESTION_COURIER_CONTRACT_USE_ESTIMATED_PRICE.onMatch(message) { (amount) -> return null }
        QUESTION_COURIER_CONFIRM.onMatch(message) { return null }
        QUESTION_DELETE_LOCATIONS_SUBFOLDER_CONFIRM.onMatch(message) { return null }
        QUESTION_DELETE_LOCATIONS_CONFIRM.onMatch(message) { (amount) -> return null }
        QUESTION_SHIP_HAS_OFFLINE_MODULES.onMatch(message) { return null }
        QUESTION_SKILL_POINTS_WILL_BE_INJECTED.onMatch(message) { return null }
        QUESTION_NON_EMPTY_SHIP_CONTRACT.onMatch(message) { return null }
        return null
    }

    private fun checkInfoMessage(message: String): GameLogAction? {
        INFO_NOT_ENOUGH_CARGO_SPACE.onMatch(message) { (required, available) -> return null }
        INFO_NOT_ENOUGH_SPACE.onMatch(message) { (required, available) -> return null }
        INFO_NOT_ENOUGH_POWERGRID.onMatch(message) { (module, required, available, total) -> return null }
        INFO_NEED_MCT.onMatch(message) { return null }
        return null
    }

    private fun checkWarningMessage(message: String): GameLogAction? {
        WARNING_ABOUT_TO_DELIVER.onMatch(message) { (character, items) -> return null }
        return null
    }

    private fun checkMiningMessage(message: String): GameLogAction? {
        MINING_MINED.onMatch(message) { return null }
        return null
    }

    private fun removeFormatting(text: String): String {
        var bracketDepth = 0
        return text.filter { char ->
            when (char) {
                '<' -> {
                    bracketDepth++
                    return@filter false
                }
                '>' -> {
                    bracketDepth--
                    return@filter false
                }
                else -> bracketDepth == 0
            }
        }
    }

    private inline fun Regex.onMatch(text: String, block: (List<String>) -> Unit) {
        val match = find(text) ?: return
        val groups = match.groups.drop(1).mapNotNull { it?.value }
        block(groups)
    }
}

private val COMBAT_HIT_REGEX = """^(?<damage>[0-9]+) from (?<target>.*) - (?<hit>.*)$""".toRegex()
private val COMBAT_MISS_REGEX = """^(?<target>.*) misses you completely$""".toRegex()
private val COMBAT_ENERGY_NEUTRALIZED_REGEX = """^(?<damage>[0-9]+) GJ energy neutralized (?<target>.*) - (?<target2>.*)$""".toRegex()
private val COMBAT_ATTACK_HIT = """^(?<damage>[0-9]+) to (?<target>.*) - (?<weapon>.*) - (?<hit>.*)$""".toRegex()
private val COMBAT_ATTACK_MISS = """^Your (?<weapon>.*) misses (?<target>.*) completely - (?<weapon2>.*)$""".toRegex()
private val COMBAT_WARP_SCRAMBLE_ATTEMPT = """^Warp scramble attempt from (?<source>.*) to (?<target>.*)!$""".toRegex()
private val COMBAT_REMOTE_SHIELD_BOOSTED = """^(?<shield>[0-9]+) remote shield boosted by (?<source>.*)$""".toRegex()
private val COMBAT_REMOTE_ARMOR_REPAIRED = """^(?<armor>[0-9]+) remote armor repaired by (?<source>.*)$""".toRegex()
private val COMBAT_REMOTE_ARMOR_REPAIRED_TO = """^(?<armor>[0-9]+) remote armor repaired to (?<target>.*)$""".toRegex()

private val BOUNTY_ADDED = """^(?<amount>.*) ISK added to next bounty payout \(payment adjusted\)$""".toRegex()

private val NOTIFY_WEAPON_RAN_OUT_OF_CHARGES = """^(?<weapon>.*) has run out of charges$""".toRegex()
private val NOTIFY_WEAPON_DEACTIVATES_TARGET_EXPLODED = """^(?<weapon>.*) deactivates as (?<target>.*) begins to explode.$""".toRegex()
private val NOTIFY_CLONE_JUMPING = """^Starting clone jumping$""".toRegex()
private val NOTIFY_DISEMBARKING_FROM_SHIP = """^Disembarking from ship$""".toRegex()
private val NOTIFY_SESSION_CHANGE_ALREADY_IN_PROGRESS = """^Session change already in progress.$""".toRegex()
private val NOTIFY_DRONES_ENGAGING = """^Drones engaging (?<target>.*)$""".toRegex()
private val NOTIFY_MODULE_DEACTIVATES_TARGET_GONE = """^(?<module>.*) deactivates as the item it was targeted at is no longer present.$""".toRegex()
private val NOTIFY_DRONES_FAIL_TOO_FAR = """^The drones fail to execute your commands as the target (?<target>.*) is not within your (?<range>.*) drone command range.$""".toRegex()
private val NOTIFY_TARGET_TOO_FAR = """^The target (?<target>.*) is too far away. It must be within (?<range>.*).$""".toRegex()
private val NOTIFY_TARGET_NOT_PRESENT = """^Targeting attempt failed as the designated object is no longer present.$""".toRegex()
private val NOTIFY_TARGET_TOO_FAR_FOR_MODULE = """^(?<target>.*) is too far away to use your (?<module>.*) on, it needs to be closer than (?<range>.*).""".toRegex()
private val NOTIFY_LOADING_CHARGES = """^Loading the (?<charge>.*) into the (?<module>.*); this will take approximately (?<time>.*).$""".toRegex()
private val NOTIFY_SHIP_STOPPING = """^Ship stopping$""".toRegex()
private val NOTIFY_CANNOT_WHILE_WARPING = """^You cannot do that while warping.$""".toRegex()
private val NOTIFY_PLEASE_WAIT = """^Please wait...$""".toRegex()
private val NOTIFY_NOT_ENOUGH_CAPACITOR = """^(?<module>.*) requires (?<required>.*) units of charge. The capacitor has only (?<remaining>.*) units.$""".toRegex()
private val NOTIFY_SALVAGING_FAILED = """^Your salvaging attempt failed this time.$""".toRegex()
private val NOTIFY_SALVAGING_FAILED_ALREADY_DONE = """^Your salvaging attempt failed because the (?<target>.*) was already salvaged.$""".toRegex()
private val NOTIFY_SALVAGING_SUCCEEDED = """^You successfully salvage from the (?<target>.*).$""".toRegex()
private val NOTIFY_SALVAGING_SUCCEEDED_EMPTY = """^Your salvager successfully completes its cycle. Unfortunately the (?<target>.*) contains nothing of value.$""".toRegex()
private val NOTIFY_MAX_TARGETS = """^You are already managing (?<max>.*) targets, as many as your ship's electronics are capable of.$""".toRegex()
private val NOTIFY_RECONNECTING_TO_DRONES = """^Attempting to reconnect with nearby drones.$""".toRegex()
private val NOTIFY_CARGO_TOO_FAR_APPROACHING = """^Cargo is too far away. Ship is on automatic approach to cargo.$""".toRegex()
private val NOTIFY_CONTAINER_TOO_FAR_TO_PLACE = """^You must be within 2500 meters of the container to place items into it.$""".toRegex()
private val NOTIFY_CONTAINER_TOO_FAR_TO_REMOVE = """^You must be within 2500 meters of the container to remove items from it.$""".toRegex()
private val NOTIFY_ATTEMPTING_TO_ACTIVE_PASSIVE_MODULE = """^You are attempting to activate a passive module$""".toRegex()
private val NOTIFY_CAN_ONLY_GROUP_TURRETS_AND_LAUNCHERS = """^You can only group turrets and launchers$""".toRegex()
private val NOTIFY_ITEM_NO_LONGER_WITHIN_REACH = """^The item is no longer within your reach.$""".toRegex()
private val NOTIFY_PI_INVALID_ROUTE = """^While the Teamsters Union applauds your support, Ground Control advises you against wasting precious time and money routing empty trucks between your facilities.$""".toRegex()
private val NOTIFY_CLOAK_CANNOT_ACTIVATE_NEARBY = """^Your cloaking systems are unable to activate due to your ship being within (?<distance>.*) of the nearby (?<nearby>.*).$""".toRegex()
private val NOTIFY_CLOAK_DEACTIVATED_NEARBY = """^Your cloak deactivates due to proximity to a nearby (?<nearby>.*).$""".toRegex()
private val NOTIFY_CLOAKED_CANNOT_TARGET = """^Your targeting attempt fails because your ship is cloaked.$""".toRegex()
private val NOTIFY_CLOAKED_CANNOT_USE_MODULE = """^Interference from the cloaking you are doing is preventing your systems from functioning at this time.$""".toRegex()
private val NOTIFY_WARPING_CANNOT_TARGET = """^Interference from the warp you are doing is preventing your sensors from getting a target lock on (?<target>.*).$""".toRegex()
private val NOTIFY_WARPING_CANNOT_USE_MODULE = """^Interference from your warp prevents your systems from functioning at this time.$""".toRegex()
private val NOTIFY_EXTERNAL_FACTORS_PREVENT_MODULE = """^External factors are preventing your (?<module>.*) from responding to this command$""".toRegex()
private val NOTIFY_CANNOT_LOAD_WHILE_ACTIVE = """^You cannot load or unload (?<module>.*) while it is active.$""".toRegex()
private val NOTIFY_NOT_ENOUGH_CHARGES_TO_LOAD_ALL = """^There are not enough charges to fully load all of your modules. Some of your modules have been left partially loaded or empty.$""".toRegex()
private val NOTIFY_SCANNER_RECALIBRATING = """^The scanner is recalibrating, please wait a second$""".toRegex()
private val NOTIFY_POCO_COOLDOWN = """^The Customs Office is still processing the paperwork from your last import/export shipment. Please wait (?<duration>.*) before submitting another request to Customs.$""".toRegex()
private val NOTIFY_CANNOT_WHILE_UNDOCKING = """^Can't do that while undocking.You should be squeezed out in (?<duration>.*).$""".toRegex()
private val NOTIFY_POCO_DRAG_FIRST = """^You must add items into the Customs Office before dragging them to a spaceport.$""".toRegex()
private val NOTIFY_PI_EXPEDITED_TRANSFER_SELECT = """^Please select one or more commodities to transfer.$""".toRegex()
private val NOTIFY_UNABLE_TO_ONLINE_MODULE_MISSING_SKILLS = """^Unable to bring (?<module>.*) online. This action requires having learned the following skills: (?<skills>.*).$""".toRegex()
private val NOTIFY_FOLLOWING_WARP = """^Following (?<character>.*) in warp$""".toRegex()
private val NOTIFY_MODULE_DEACTIVATES_RESOURCE_GONE = """^(?<module>.*) deactivates as it finds the resource it was harvesting a pale shadow of its former glory.$""".toRegex()
private val NOTIFY_ALL_DRONES_RETURNING = """^All drones returning to drone bay$""".toRegex()
private val NOTIFY_REQUESTED_TO_DOCK = """^Requested to dock at (?<station>.*) station$""".toRegex()
private val NOTIFY_SETTING_COURSE_TO_DOCK = """^Setting course to docking perimeter$""".toRegex()
private val NOTIFY_DOCKING_REQUEST_ACCEPTED = """^Your docking request has been accepted. Your ship will be towed into station.$""".toRegex()
private val NOTIFY_CANNOT_WHILE_DOCKING = """^You cannot do that while docking.$""".toRegex()
private val NOTIFY_DOCKING_ALREADY_IN_PROGRESS = """^Docking operation already in progress.Estimated time left: (?<time>.*).$""".toRegex()
private val NOTIFY_CHARACTER_AGGRO_SENTRY_GUNS = """^(?<character>.*): You have foolishly engaged in criminal activity within sight of sentry guns and must suffer the consequences.$""".toRegex()
private val NOTIFY_COMMAND_BURST_BONUSES = """^Your (?<module>.*) has applied bonuses to (?<amount>.*) fleet members.$""".toRegex()
private val NOTIFY_PI_BUSY = """^The planetary communications network is busy. Please wait (?<time>.*).$""".toRegex()

private val HINT_ATTEMPTING_TO_JOIN_A_CHANNEL = """^Attempting to join a channel$""".toRegex()
private val HINT_MODULE_ALREADY_ACTIVE = """^(?<module>.*) is already active$""".toRegex()

private val NONE_JUMPING = """^Jumping from (?<from>.*) to (?<to>.*)$""".toRegex()
private val NONE_UNDOCKING = """^Undocking from (?<from>.*) to (?<to>.*) solar system.$""".toRegex()

private val QUESTION_REMOVE_LOCATION = """^Are you sure you want to remove this location (?<location>.*)\?$""".toRegex()
private val QUESTION_SELF_DESTRUCT = """^Would you like to self-destruct your capsule and transfer to a new clone at (?<location>.*)\?$""".toRegex()
private val QUESTION_QUIT_GAME = """^Are you sure you want to quit the game\?$""".toRegex()
private val QUESTION_ITEMS_REDEEMED = """^The following items will be redeemed, activated and applied to (?<character>.*) (?<items>.*).$""".toRegex()
private val QUESTION_EXIT_SIMULATION = """^You are about to exit simulation mode. Any progress you have made on your currently simulated ship will be lost.Are you sure you want to continue\?$""".toRegex()
private val QUESTION_REPACKAGE = """^Are you sure that you want to repackage this item\?$""".toRegex()
private val QUESTION_COURIER_CONTRACT_LARGE = """^This courier contract has (?<size>.*) m3 worth of merchandise. Only a ship with an exceptionally large cargo hold will be able to transport this cargo. Are you sure you want to continue\?$""".toRegex()
private val QUESTION_COURIER_CONTRACT_USE_ESTIMATED_PRICE = """^Do you want to use the estimated price for your items which is (?<amount>.*) ISK\?$""".toRegex()
private val QUESTION_COURIER_CONFIRM = """^Are you sure you want to create this contract\? Make sure you have read the information on the confirmation page carefully before clicking 'yes'. Any fees that are incurred with setting up this contract are non-refundable.$""".toRegex()
private val QUESTION_DELETE_LOCATIONS_SUBFOLDER_CONFIRM = """^Are you sure you want to delete this subfolder and all of its contents\?$""".toRegex()
private val QUESTION_DELETE_LOCATIONS_CONFIRM = """^Are you sure you want to remove (?<amount>.*) locations\?$""".toRegex()
private val QUESTION_SHIP_HAS_OFFLINE_MODULES = """^One or more module on your ship is offline. This may be due to you not having the skill requirements for operating the module. Modules have no functionality while offline. Penalties may still apply.$""".toRegex()
private val QUESTION_SKILL_POINTS_WILL_BE_INJECTED = """^(?<skillpoints>.*) Skill Points will be redeemed and directly injected to (?<character>.*).$""".toRegex()
private val QUESTION_NON_EMPTY_SHIP_CONTRACT = """^You are entering a non-empty ship into this contract. The ship and all its items will be entered into the contract.The ship (?<ship>.*) contains the following items: (?<items>.*)$""".toRegex()

private val INFO_NOT_ENOUGH_CARGO_SPACE = """^(?<required>.*) cargo units would be required to complete this operation. Destination container only has (?<available>.*) units available.$""".toRegex()
private val INFO_NOT_ENOUGH_SPACE = """^(?<required>.*) units of space would be required to complete this operation. Destination container only has (?<available>.*) units available.$""".toRegex()
private val INFO_NOT_ENOUGH_POWERGRID = """^To bring (?<module>.*) online requires (?<required>.*) power units, but only (?<available>.*) of the (?<total>.*) units that your power core produces are still available.$""".toRegex()
private val INFO_NEED_MCT = """^Another character owned by this account is training a skill. Only one character can train a skill per account.$""".toRegex()

private val WARNING_ABOUT_TO_DELIVER = """^You are about to deliver the following items to (?<character>.*):(?<items>.*)Are you sure you want to do this\?$""".toRegex()

private val MINING_MINED = """^You mined (?<amount>.*) units of (?<resource>.*) with a lost residue of (?<residue>.*) units$""".toRegex()
