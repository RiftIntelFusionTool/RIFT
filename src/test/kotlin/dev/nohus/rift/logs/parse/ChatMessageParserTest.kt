package dev.nohus.rift.logs.parse

import dev.nohus.rift.logs.parse.ChatMessageParser.KeywordType.Clear
import dev.nohus.rift.logs.parse.ChatMessageParser.KeywordType.GateCamp
import dev.nohus.rift.logs.parse.ChatMessageParser.KeywordType.NoVisual
import dev.nohus.rift.logs.parse.ChatMessageParser.QuestionType.Location
import dev.nohus.rift.logs.parse.ChatMessageParser.QuestionType.ShipTypes
import dev.nohus.rift.logs.parse.ChatMessageParser.Token
import dev.nohus.rift.logs.parse.ChatMessageParser.TokenType
import dev.nohus.rift.logs.parse.ChatMessageParser.TokenType.Count
import dev.nohus.rift.logs.parse.ChatMessageParser.TokenType.Gate
import dev.nohus.rift.logs.parse.ChatMessageParser.TokenType.Keyword
import dev.nohus.rift.logs.parse.ChatMessageParser.TokenType.Kill
import dev.nohus.rift.logs.parse.ChatMessageParser.TokenType.Link
import dev.nohus.rift.logs.parse.ChatMessageParser.TokenType.Movement
import dev.nohus.rift.logs.parse.ChatMessageParser.TokenType.Player
import dev.nohus.rift.logs.parse.ChatMessageParser.TokenType.Question
import dev.nohus.rift.logs.parse.ChatMessageParser.TokenType.Ship
import dev.nohus.rift.logs.parse.ChatMessageParser.TokenType.System
import dev.nohus.rift.logs.parse.ChatMessageParser.TokenType.Url
import dev.nohus.rift.repositories.CharactersRepository
import dev.nohus.rift.repositories.CharactersRepository.CharacterState
import dev.nohus.rift.repositories.ShipTypesRepository
import dev.nohus.rift.repositories.SolarSystemsRepository
import dev.nohus.rift.repositories.WordsRepository
import io.kotest.core.spec.IsolationMode
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.comparables.shouldBeLessThan
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.measureTimedValue

class ChatMessageParserTest : FreeSpec({

    isolationMode = IsolationMode.InstancePerTest

    val mockSolarSystemsRepository: SolarSystemsRepository = mockk()
    val mockShipTypesRepository: ShipTypesRepository = mockk()
    val mockCharactersRepository: CharactersRepository = mockk()
    val mockWordsRepository: WordsRepository = mockk()
    val characterNameValidator = CharacterNameValidator()
    val target = ChatMessageParser(
        mockSolarSystemsRepository,
        mockShipTypesRepository,
        mockCharactersRepository,
        mockWordsRepository,
        characterNameValidator,
    )
    every { mockSolarSystemsRepository.getSystemName(any(), eq("Delve")) } returns null
    every { mockShipTypesRepository.getShip(any()) } returns null
    coEvery { mockCharactersRepository.getCharacterNamesStatus(any()) } returns emptyMap()
    every { mockWordsRepository.isWord(any()) } returns false

    "system link, player link, player" {
        every { mockSolarSystemsRepository.getSystemName("D-W7F0", "Delve") } returns "D-W7F0"
        coEvery { mockCharactersRepository.getCharacterNamesStatus(any()) } returns listOf("Ishani Kalki", "Shiva Callipso").existing()

        val actual = target.parse("D-W7F0  Ishani Kalki  Shiva Callipso", "Delve")

        actual shouldContain listOf(
            "D-W7F0".token(System("D-W7F0"), Link),
            "Ishani Kalki".token(Player(0), Link),
            "Shiva Callipso".token(Player(0)),
        )
    }

    "player, ship" {
        every { mockShipTypesRepository.getShip("malediction") } returns "Malediction"
        coEvery { mockCharactersRepository.getCharacterNamesStatus(any()) } returns listOf("S-Killer").existing()

        val actual = target.parse("S-Killer malediction", "Delve")

        actual shouldContain listOf(
            "S-Killer".token(Player(0)),
            "malediction".token(Ship("Malediction")),
        )
    }

    "system clear" {
        every { mockSolarSystemsRepository.getSystemName("319-3D", "Delve") } returns "319-3D"

        val actual = target.parse("319-3D clr", "Delve")

        actual shouldContain listOf(
            "319-3D".token(System("319-3D")),
            "clr".token(Keyword(Clear)),
        )
    }

    "player, extra spaces, system, clear" {
        every { mockSolarSystemsRepository.getSystemName("MO-GZ5", "Delve") } returns "MO-GZ5"
        coEvery { mockCharactersRepository.getCharacterNamesStatus(any()) } returns listOf("Rinah Minayin").existing()

        val actual = target.parse("Rinah Minayin   MO-GZ5 nv", "Delve")

        actual shouldContain listOf(
            "Rinah Minayin".token(Player(0), Link),
            "MO-GZ5".token(System("MO-GZ5")),
            "nv".token(Keyword(NoVisual)),
        )
    }

    "system with star" {
        every { mockSolarSystemsRepository.getSystemName("N-8YET", "Delve") } returns "N-8YET"
        coEvery { mockCharactersRepository.getCharacterNamesStatus(any()) } returns listOf("Charlie Murdoch").existing()

        val actual = target.parse("N-8YET*  Charlie Murdoch", "Delve")

        actual shouldContain listOf(
            "N-8YET".token(System("N-8YET"), Link),
            "Charlie Murdoch".token(Player(0)),
        )
    }

    "system with star, clear" {
        every { mockSolarSystemsRepository.getSystemName("N-8YET", "Delve") } returns "N-8YET"

        val actual = target.parse("N-8YET* clr", "Delve")

        actual shouldContain listOf(
            "N-8YET".token(System("N-8YET"), Link),
            "clr".token(Keyword(Clear)),
        )
    }

    "ship with star, player, system with star" {
        every { mockSolarSystemsRepository.getSystemName("NOL-M9", "Delve") } returns "NOL-M9"
        every { mockShipTypesRepository.getShip("Caldari Shuttle") } returns "Caldari Shuttle"
        coEvery { mockCharactersRepository.getCharacterNamesStatus(any()) } returns listOf("Keeppley TT").existing()

        val actual = target.parse("Caldari Shuttle*  Keeppley TT  NOL-M9*", "Delve")

        actual shouldContain listOf(
            "Caldari Shuttle".token(Ship("Caldari Shuttle"), Link),
            "Keeppley TT".token(Player(0), Link),
            "NOL-M9".token(System("NOL-M9"), Link),
        )
    }

    "player, system, ship" {
        every { mockSolarSystemsRepository.getSystemName("SVM-3K", "Delve") } returns "SVM-3K"
        every { mockShipTypesRepository.getShip("eris") } returns "Eris"
        coEvery { mockCharactersRepository.getCharacterNamesStatus(any()) } returns listOf("M2002M").existing()

        val actual = target.parse("M2002M  SVM-3K eris", "Delve")

        actual shouldContain listOf(
            "M2002M".token(Player(0), Link),
            "SVM-3K".token(System("SVM-3K")),
            "eris".token(Ship("Eris")),
        )
    }

    "player link, player, count, ship link, system" {
        every { mockSolarSystemsRepository.getSystemName("319-3D", "Delve") } returns "319-3D"
        every { mockShipTypesRepository.getShip("capsule") } returns "Capsule"
        coEvery { mockCharactersRepository.getCharacterNamesStatus(any()) } returns listOf("ssllss1", "Yaakov Y2").existing()

        val actual = target.parse("ssllss1  Yaakov Y2 2x capsule  319-3D", "Delve")

        actual shouldContain listOf(
            "ssllss1".token(Player(0), Link),
            "Yaakov Y2".token(Player(0)),
            "2x capsule".token(Ship("Capsule", count = 2), Link),
            "319-3D".token(System("319-3D")),
        )
    }

    "plural ships" {
        every { mockShipTypesRepository.getShip("capsule") } returns "Capsule"

        val actual = target.parse("both capsules", "Delve")

        actual shouldContain listOf(
            "both capsules".token(Ship("Capsule", count = 2, isPlural = true)),
        )
    }

    "player, question" {
        coEvery { mockCharactersRepository.getCharacterNamesStatus(any()) } returns listOf("Ishani Kalki").existing()

        val actual = target.parse("Ishani Kalki where is he", "Delve")

        actual shouldContain listOf(
            "Ishani Kalki".token(Player(0)),
            "where is he".token(Question(Location, "where is he")),
        )
    }

    "plus player" {
        coEvery { mockCharactersRepository.getCharacterNamesStatus(any()) } returns listOf("ssllss1").existing()

        val actual = target.parse("+ ssllss1", "Delve")

        actual shouldContain listOf(
            "+".token(),
            "ssllss1".token(Player(0)),
        )
    }

    "plain word" {
        val actual = target.parse("rgr", "Delve")

        actual shouldContain listOf(
            "rgr".token(),
        )
    }

    "system, complex text, shortened system" {
        // TODO: More complexity here
        every { mockSolarSystemsRepository.getSystemName("MO-GZ5", "Delve") } returns "MO-GZ5"
        every { mockSolarSystemsRepository.getSystemName("1dq", "Delve") } returns "1DQ1-A"

        val actual = target.parse("MO-GZ5 neutrals in 1dq on Mo gate", "Delve")

        actual shouldContain listOf(
            "MO-GZ5".token(System("MO-GZ5")),
            "neutrals in".token(),
            "1dq".token(System("1DQ1-A")),
            "on Mo gate".token(),
        )
    }

    "ship count, changing capital text" {
        every { mockShipTypesRepository.getShip("wreaTH") } returns "Wreath"
        every { mockShipTypesRepository.getShip("LOKI") } returns "Loki"

        val actual = target.parse("2 wreaTH AND A LOKI", "Delve")

        actual shouldContain listOf(
            "2 wreaTH".token(Ship("Wreath", count = 2)),
            "AND A".token(),
            "LOKI".token(Ship("Loki")),
        )
    }

    "negative ship count" {
        every { mockShipTypesRepository.getShip("loki") } returns "Loki"

        val actual = target.parse("-3 loki", "Delve")

        actual shouldContain listOf(
            "-3".token(),
            "loki".token(Ship("Loki")),
        )
    }

    "player, plus count, system" {
        every { mockSolarSystemsRepository.getSystemName("ZXB-VC", "Delve") } returns "ZXB-VC"
        coEvery { mockCharactersRepository.getCharacterNamesStatus(any()) } returns listOf("stark").existing()

        val actual = target.parse("stark +3 ZXB-VC", "Delve")

        actual shouldContain listOf(
            "stark".token(Player(0)),
            "+3".token(Count(3, isPlus = true)),
            "ZXB-VC".token(System("ZXB-VC")),
        )
    }

    "plus count, system, ship" {
        every { mockSolarSystemsRepository.getSystemName("ZXB-VC", "Delve") } returns "ZXB-VC"
        every { mockShipTypesRepository.getShip("hecate") } returns "Hecate"

        val actual = target.parse("+5  ZXB-VC hecate", "Delve")

        actual shouldContain listOf(
            "+5".token(Count(5, isPlus = true), Link),
            "ZXB-VC".token(System("ZXB-VC")),
            "hecate".token(Ship("Hecate")),
        )
    }

    "plus count with space, system, ship" {
        every { mockSolarSystemsRepository.getSystemName("ZXB-VC", "Delve") } returns "ZXB-VC"
        every { mockShipTypesRepository.getShip("hecate") } returns "Hecate"

        val actual = target.parse("+ 5  ZXB-VC hecate", "Delve")

        actual shouldContain listOf(
            "+ 5".token(Count(5, isPlus = true), Link),
            "ZXB-VC".token(System("ZXB-VC")),
            "hecate".token(Ship("Hecate")),
        )
    }

    "shiptypes question" {
        every { mockSolarSystemsRepository.getSystemName("ZXB-VC", "Delve") } returns "ZXB-VC"

        val actual = target.parse("ZXB-VC those +5 do we know other shiptypes?", "Delve")

        actual shouldContain listOf(
            "ZXB-VC".token(System("ZXB-VC")),
            "those".token(),
            "+5".token(Count(5, isPlus = true)),
            "do we know other".token(),
            "shiptypes?".token(Question(ShipTypes, "shiptypes?")),
        )
    }

    "comment" {
        // TODO: More complexity here
        every { mockShipTypesRepository.getShip("shuttle") } returns "Shuttle"
        every { mockShipTypesRepository.getShip("pod") } returns "Capsule"

        val actual = target.parse("we have a lot of shuttle and pod movement of fraand mohiz in npc today", "Delve")

        actual shouldContain listOf(
            "we have a lot of".token(),
            "shuttle".token(Ship("Shuttle")),
            "and".token(),
            "pod".token(Ship("Capsule")),
            "movement of fraand mohiz in npc today".token(),
        )
    }

    "killmail" {
        val actual = target.parse("Kill: super-ego (Hecate)", "Delve")

        actual shouldContain listOf(
            "Kill: super-ego (Hecate)".token(Kill(name = "super-ego", characterId = null, target = "Hecate")),
        )
    }

    "system link, player, plus count, count, ship, comma, count, keyword" {
        every { mockSolarSystemsRepository.getSystemName("319-3D", "Delve") } returns "319-3D"
        every { mockShipTypesRepository.getShip("hecate") } returns "Hecate"
        coEvery { mockCharactersRepository.getCharacterNamesStatus(any()) } returns listOf("RB Charlote").existing()

        val actual = target.parse("319-3D  RB Charlote +3 1x hecate, 3x nv", "Delve")

        actual shouldContain listOf(
            "319-3D".token(System("319-3D"), Link),
            "RB Charlote".token(Player(0)),
            "+3".token(Count(3, isPlus = true)),
            "1x hecate".token(Ship("Hecate", count = 1)),
            "3x".token(Count(3)),
            "nv".token(Keyword(NoVisual)),
        )
    }

    "player link, player, text, system, ship names" {
        every { mockSolarSystemsRepository.getSystemName("1-2J4P", "Delve") } returns "1-2J4P"
        every { mockShipTypesRepository.getShip("purifier") } returns "Purifier"
        every { mockShipTypesRepository.getShip("sabre") } returns "Sabre"
        coEvery { mockCharactersRepository.getCharacterNamesStatus(any()) } returns listOf("FeiShi", "iT0p").existing()

        val actual = target.parse("FeiShi  iT0p camping in 1-2J4P purifier + sabre", "Delve")

        actual shouldContain listOf(
            "FeiShi".token(Player(0), Link),
            "iT0p".token(Player(0)),
            "camping in".token(),
            "1-2J4P".token(System("1-2J4P")),
            "purifier".token(Ship("Purifier")),
            "+".token(),
            "sabre".token(Ship("Sabre")),
        )
    }

    "system, text, url" {
        every { mockSolarSystemsRepository.getSystemName("Q-JQSG", "Delve") } returns "Q-JQSG"

        val actual = target.parse("Q-JQSG clearing https://adashboard.info/intel/dscan/view/D91snCmT", "Delve")

        actual shouldContain listOf(
            "Q-JQSG".token(System("Q-JQSG")),
            "clearing".token(),
            "https://adashboard.info/intel/dscan/view/D91snCmT".token(Url),
        )
    }

    "long comment" {
        val (actual, time) = measureTimedValue {
            target.parse("if you want to know if your +1/-1 is around look at watch list it will have their ship symbol on the list next to their name if they are on grid", "Delve")
        }

        actual shouldContain listOf(
            "if you want to know if your +1/-1 is around look at watch list it will have their ship symbol on the list next to their name if they are on grid".token(),
        )
        actual shouldHaveSize 1
        time shouldBeLessThan 200.milliseconds
    }

    "hostile crafted text" {
        // Text specifically crafted for branching ambiguity and an overwhelming amount of possible tokenizations
        coEvery { mockCharactersRepository.getCharacterNamesStatus(any()) } returns listOf("aaa", "aaa aaa", "aaa aaa aaa").existing()

        val (actual, time) = measureTimedValue {
            target.parse("aaa aaa aaa aaa aaa aaa aaa aaa aaa aaa aaa aaa aaa aaa aaa", "Delve")
        }

        actual shouldContain listOf(
            "aaa aaa aaa aaa aaa aaa aaa aaa aaa aaa aaa aaa aaa aaa aaa".token(),
        )
        actual shouldHaveSize 1
        time shouldBeLessThan 1000.milliseconds
    }

    "space at start of message" {
        val actual = target.parse(" RGC for CSM 18", "Delve")

        actual shouldContain listOf(
            "RGC for CSM 18".token(),
        )
        actual shouldHaveSize 1
    }

    "many linked characters, including 3 word names" {
        coEvery { mockCharactersRepository.getCharacterNamesStatus(any()) } returns listOf("CPT Grabowsky", "Kelci Papi", "Kelio Rift", "Rim'tuti'tuks", "Lucho IYI", "Shopa s topa", "Urriah Souldown").existing()

        val actual = target.parse("CPT Grabowsky  Kelci Papi  Kelio Rift  Rim'tuti'tuks  Lucho IYI  Shopa s topa  Urriah Souldown", "Delve")

        actual.forEach { println(it) }
        actual shouldContain listOf(
            "CPT Grabowsky".token(Player(0), Link),
            "Kelci Papi".token(Player(0), Link),
            "Kelio Rift".token(Player(0), Link),
            "Rim'tuti'tuks".token(Player(0), Link),
            "Lucho IYI".token(Player(0), Link),
            "Shopa s topa".token(Player(0), Link),
            "Urriah Souldown".token(Player(0)),
        )
    }

    "plain text link" {
        val actual = target.parse("maybe its hourly  ", "Delve")

        actual shouldContain listOf(
            "maybe its hourly".token(),
        )
        actual shouldHaveSize 1
    }

    "character-like plain text" {
        coEvery { mockCharactersRepository.getCharacterNamesStatus(any()) } returns listOf("was", "the blues").existing()
        every { mockWordsRepository.isWord("was") } returns true
        every { mockWordsRepository.isWord("the") } returns true
        every { mockWordsRepository.isWord("blues") } returns true

        val actual = target.parse("nothing of value was lost it's the blues", "Delve")

        actual shouldContain listOf(
            "nothing of value was lost it's the blues".token(),
        )
        actual shouldHaveSize 1
    }

    "system gate" {
        coEvery { mockCharactersRepository.getCharacterNamesStatus(any()) } returns listOf("Ruthy").existing()
        every { mockShipTypesRepository.getShip("stabber") } returns "Stabber"
        every { mockSolarSystemsRepository.getSystemName("uvho", "Delve") } returns "UVHO-F"

        val actual = target.parse("Ruthy stabber uvho gate", "Delve")

        actual shouldContain listOf(
            "Ruthy".token(Player(0)),
            "stabber".token(Ship("Stabber")),
            "uvho gate".token(Gate("UVHO-F")),
        )
    }

    "system gate with more text" {
        every { mockSolarSystemsRepository.getSystemName("k7", "Delve") } returns "K7D-II"
        every { mockSolarSystemsRepository.getSystemName("OGY", "Delve") } returns "OGY-6D"

        val actual = target.parse("moved away from k7 gate in OGY", "Delve")

        actual shouldContain listOf(
            "moved away from".token(),
            "k7 gate".token(Gate("K7D-II")),
            "in".token(),
            "OGY".token(System("OGY-6D")),
        )
    }

    "gate system" {
        coEvery { mockCharactersRepository.getCharacterNamesStatus(any()) } returns listOf("CrystalWater").existing()
        every { mockShipTypesRepository.getShip("Retribution") } returns "Retribution"
        every { mockSolarSystemsRepository.getSystemName("FM-JK5", "Delve") } returns "FM-JK5"
        every { mockSolarSystemsRepository.getSystemName("JP4", "Delve") } returns "JP4-AA"

        val actual = target.parse("FM-JK5  CrystalWater +10 gate JP4  Retribution", "Delve")

        actual shouldContain listOf(
            "FM-JK5".token(System("FM-JK5"), Link),
            "CrystalWater".token(Player(0)),
            "+10".token(Count(10, isPlus = true)),
            "gate JP4".token(Gate("JP4-AA"), Link),
            "Retribution".token(Ship("Retribution")),
        )
    }

    "going system" {
        coEvery { mockCharactersRepository.getCharacterNamesStatus(any()) } returns listOf("CrystalWater").existing()
        every { mockShipTypesRepository.getShip("Retribution") } returns "Retribution"
        every { mockSolarSystemsRepository.getSystemName("FM-JK5", "Delve") } returns "FM-JK5"
        every { mockSolarSystemsRepository.getSystemName("JP4", "Delve") } returns "JP4-AA"

        val actual = target.parse("FM-JK5  CrystalWater going JP4", "Delve")

        actual shouldContain listOf(
            "FM-JK5".token(System("FM-JK5"), Link),
            "CrystalWater".token(Player(0)),
            "going JP4".token(Movement("going", "JP4-AA", isGate = false)),
        )
    }

    "jumped system" {
        coEvery { mockCharactersRepository.getCharacterNamesStatus(any()) } returns listOf("CrystalWater").existing()
        every { mockShipTypesRepository.getShip("Retribution") } returns "Retribution"
        every { mockSolarSystemsRepository.getSystemName("FM-JK5", "Delve") } returns "FM-JK5"
        every { mockSolarSystemsRepository.getSystemName("JP4", "Delve") } returns "JP4-AA"

        val actual = target.parse("FM-JK5  CrystalWater jumped JP4", "Delve")

        actual shouldContain listOf(
            "FM-JK5".token(System("FM-JK5"), Link),
            "CrystalWater".token(Player(0)),
            "jumped JP4".token(Movement("jumped", "JP4-AA", isGate = false)),
        )
    }

    "jumped gate" {
        coEvery { mockCharactersRepository.getCharacterNamesStatus(any()) } returns listOf("CrystalWater").existing()
        every { mockShipTypesRepository.getShip("Retribution") } returns "Retribution"
        every { mockSolarSystemsRepository.getSystemName("FM-JK5", "Delve") } returns "FM-JK5"
        every { mockSolarSystemsRepository.getSystemName("JP4", "Delve") } returns "JP4-AA"

        val actual = target.parse("FM-JK5  CrystalWater jumped JP4 gate", "Delve")

        actual shouldContain listOf(
            "FM-JK5".token(System("FM-JK5"), Link),
            "CrystalWater".token(Player(0)),
            "jumped JP4 gate".token(Movement("jumped", "JP4-AA", isGate = true)),
        )
    }

    "system ansiblex" {
        every { mockSolarSystemsRepository.getSystemName("1DQ", "Delve") } returns "1DQ1-A"

        val actual = target.parse("on 1DQ ansi", "Delve")

        actual shouldContain listOf(
            "on".token(),
            "1DQ ansi".token(Gate("1DQ1-A", isAnsiblex = true)),
        )
    }

    "gate and gate camp" {
        coEvery { mockCharactersRepository.getCharacterNamesStatus(any()) } returns listOf("chazzathespazman", "camp").existing()
        every { mockSolarSystemsRepository.getSystemName("B-DBYQ", "Delve") } returns "B-DBYQ"
        every { mockSolarSystemsRepository.getSystemName("J5A-IX", "Delve") } returns "J5A-IX"

        val actual = target.parse("chazzathespazman +7  B-DBYQ gate camp on  J5A-IX gate", "Delve")

        actual shouldContain listOf(
            "chazzathespazman".token(Player(0)),
            "+7".token(Count(7, isPlus = true), Link),
            "B-DBYQ".token(System("B-DBYQ")),
            "gate camp".token(Keyword(GateCamp)),
            "on".token(),
            "J5A-IX gate".token(Gate("J5A-IX")),
        )
    }

    "space" {
        val actual = target.parse(" ", "Delve")
        actual.shouldBeEmpty()
    }

    "comma" {
        val actual = target.parse(",", "Delve")
        actual.shouldBeEmpty()
    }

    "player name matching an out-of-region system name" {
        coEvery { mockCharactersRepository.getCharacterNamesStatus(any()) } returns listOf("KQK").existing()
        every { mockSolarSystemsRepository.getSystemName("KQK", "Delve") } returns "KQK1-2"
        every { mockSolarSystemsRepository.getRegionBySystem("KQK1-2") } returns "Pure Blind"
        every { mockSolarSystemsRepository.getSystemName("1-SMEB", "Delve") } returns "1-SMEB"

        val actual = target.parse("KQK  1-SMEB", "Delve")

        actual shouldContain listOf(
            "KQK".token(Player(0)),
            "1-SMEB".token(System("1-SMEB")),
        )
    }

    "player name matching an in-region system name" {
        coEvery { mockCharactersRepository.getCharacterNamesStatus(any()) } returns listOf("KQK").existing()
        every { mockSolarSystemsRepository.getSystemName("KQK", "Delve") } returns "KQK1-2"
        every { mockSolarSystemsRepository.getRegionBySystem("KQK1-2") } returns "Delve"
        every { mockSolarSystemsRepository.getSystemName("1-SMEB", "Delve") } returns "1-SMEB"

        val actual = target.parse("KQK  1-SMEB", "Delve")

        actual shouldContain listOf(
            "KQK".token(System("KQK1-2")),
            "1-SMEB".token(System("1-SMEB")),
        )
    }

    "player name substring matching a system name" {
        coEvery { mockCharactersRepository.getCharacterNamesStatus(any()) } returns listOf("Jita Alt 1").existing()
        every { mockSolarSystemsRepository.getSystemName("Jita", "Delve") } returns "Jita"

        val actual = target.parse("Jita Alt 1", "Delve")

        actual shouldContain listOf(
            "Jita Alt 1".token(Player(0)),
        )
    }

    "system, player name substring matching a system name" {
        coEvery { mockCharactersRepository.getCharacterNamesStatus(any()) } returns listOf("Jita Alt 1").existing()
        every { mockSolarSystemsRepository.getSystemName("Jita", "Delve") } returns "Jita"

        val actual = target.parse("Jita Jita Alt 1", "Delve")

        actual shouldContain listOf(
            "Jita".token(System("Jita")),
            "Jita Alt 1".token(Player(0)),
        )
    }
})

private fun String.token(vararg types: TokenType): Token {
    return Token(split(" "), types = types.toList())
}

private fun List<String>.existing(): Map<String, CharacterState> {
    return associateWith { CharacterState.Exists(0) }
}
