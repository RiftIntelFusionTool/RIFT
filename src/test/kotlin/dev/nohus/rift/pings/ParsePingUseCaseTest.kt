package dev.nohus.rift.pings

import dev.nohus.rift.configurationpack.ConfigurationPackRepository
import dev.nohus.rift.repositories.CharactersRepository
import dev.nohus.rift.repositories.MapStatusRepository
import dev.nohus.rift.repositories.SolarSystemsRepository
import io.kotest.core.spec.IsolationMode
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.equality.FieldsEqualityCheckConfig
import io.kotest.matchers.equality.shouldBeEqualToComparingFields
import io.kotest.matchers.nulls.shouldNotBeNull
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import java.time.Instant

class ParsePingUseCaseTest : FreeSpec({

    isolationMode = IsolationMode.InstancePerTest

    val timestamp = Instant.now()
    val mockCharactersRepository: CharactersRepository = mockk()
    val mockSolarSystemsRepository: SolarSystemsRepository = mockk()
    val mockConfigurationPackRepository: ConfigurationPackRepository = mockk()
    val mockMapStatusRepository: MapStatusRepository = mockk()
    val target = ParsePingUseCase(
        charactersRepository = mockCharactersRepository,
        solarSystemsRepository = mockSolarSystemsRepository,
        configurationPackRepository = mockConfigurationPackRepository,
        mapStatusRepository = mockMapStatusRepository,
    )

    coEvery { mockCharactersRepository.getCharacterId("Havish Montak") } returns 1
    coEvery { mockCharactersRepository.getCharacterId("Mrbluff343") } returns 2
    coEvery { mockCharactersRepository.getCharacterId("Mist Amatin") } returns 3
    coEvery { mockCharactersRepository.getCharacterId("Asher Elias") } returns 4
    every { mockSolarSystemsRepository.getSystemName("1DQ1-A", null) } returns "1DQ1-A"
    every { mockSolarSystemsRepository.getSystemName("U-Q", null) } returns null
    every { mockSolarSystemsRepository.getSystemName("U-Q", null, listOf(30000629)) } returns "U-QMOA"
    every { mockConfigurationPackRepository.getFriendlyAllianceIds() } returns listOf(1)
    every { mockMapStatusRepository.status } returns mockk {
        every { value } returns mapOf(
            30000629 to mockk { // U-QMOA
                every { sovereignty } returns mockk {
                    every { allianceId } returns 1
                }
            },
            30001155 to mockk { // U-QVWD
                every { sovereignty } returns mockk {
                    every { allianceId } returns 2
                }
            },
        )
    }

    listOf(
        """
            Single line
            ~~~ This was a guardbees broadcast from toaster_jane to all at 2024-01-25 02:18:57.549510 EVE ~~~
        """.trimIndent() to PingModel.PlainText(
            timestamp = timestamp,
            sourceText = "",
            text = "Single line",
            sender = "toaster_jane",
            target = "all",
        ),
        """
            Single‍‍‍‍‍‍‍ line
            ~~~ This was a guardbees broadcast from toaster_jane to all at 2024-01-25 02:18:57.549510 EVE ~~~
        """.trimIndent() to PingModel.PlainText(
            timestamp = timestamp,
            sourceText = "",
            text = "Single line",
            sender = "toaster_jane",
            target = "all",
        ),
        """
            more sif into beehive on mal plox now

            ~~~ This was a guardbees broadcast from toaster_jane to all at 2024-01-25 02:18:57.549510 EVE ~~~
        """.trimIndent() to PingModel.PlainText(
            timestamp = timestamp,
            sourceText = "",
            text = "more sif into beehive on mal plox now",
            sender = "toaster_jane",
            target = "all",
        ),
        """
            Hostiles need some time to dock and spin ships. Bring tackle and hunters. NEUTS on sentinels too.

            FC Name: Havish Montak
            Formup Location: 1DQ1-A
            PAP Type: Strategic
            Comms: Op 4 https://gnf.lt/2eMgwE2.html
            Doctrine: Void Rays (MWD) (Boosts > Logi > Kikis > Hawk/Slasher/Hyena/Keres)

            ~~~ This was a coord broadcast from dakota_holtgard to all at 2024-01-22 18:43:14.530878 EVE ~~~
        """.trimIndent() to PingModel.FleetPing(
            timestamp = timestamp,
            sourceText = "",
            description = "Hostiles need some time to dock and spin ships. Bring tackle and hunters. NEUTS on sentinels too.",
            fleetCommander = FleetCommander("Havish Montak", 1),
            fleet = null,
            formupLocations = listOf(FormupLocation.System("1DQ1-A")),
            papType = PapType.Strategic,
            comms = Comms.Mumble("Op 4", "https://gnf.lt/2eMgwE2.html"),
            doctrine = Doctrine(
                text = "Void Rays (MWD) (Boosts > Logi > Kikis > Hawk/Slasher/Hyena/Keres)",
                link = "https://goonfleet.com/index.php/topic/345055-active-strat-void-rays-mwd-kikis/",
            ),
            broadcastSource = "coord",
            target = "all",
        ),
        """
            WTF 205 - Command Destroyers  

            What mindlink do you need to boost? How the hell does booshing work? Why do I have a combat timer on this gate? Find the answers to all these questions and more in this class.  

            FC: Mrbluff343 
            Fleet: WTF 205 
            Formup: 1DQ1-A PAP 
            Type: Peacetime 
            Comms: General Doctrine: Pontifex/Stork/Bifrost/Magus/Draugur - Ensure you have a MJFG fitted.  

            Please note there is no SRP for Gooniversity Classes. Our 200 series covers advanced topics, so bring your own ships at your own risk.
            
            ~~~ This was a broadcast from ankh_lai to gooniversity at 2024-01-20 23:09:29.893448 EVE ~~~
        """.trimIndent() to PingModel.FleetPing(
            timestamp = timestamp,
            sourceText = "",
            description = "WTF 205 - Command Destroyers\n\nWhat mindlink do you need to boost? How the hell does booshing work? Why do I have a combat timer on this gate? Find the answers to all these questions and more in this class.\n\nPlease note there is no SRP for Gooniversity Classes. Our 200 series covers advanced topics, so bring your own ships at your own risk.",
            fleetCommander = FleetCommander("Mrbluff343", 2),
            fleet = "WTF 205",
            formupLocations = listOf(FormupLocation.System("1DQ1-A")),
            papType = PapType.Peacetime,
            comms = Comms.Text("General"),
            doctrine = Doctrine(
                text = "Pontifex/Stork/Bifrost/Magus/Draugur - Ensure you have a MJFG fitted.",
                link = null,
            ),
            broadcastSource = null,
            target = "gooniversity",
        ),
        """
            I found an absolutely amazing wh rout of awesome. Getin, im itching for some blood!
    
            FC Name: Mist Amatin
            Formup Location: 1DQ1-A
            PAP Type: Strategic
            Comms: Op 3 https://gnf.lt/NOH1FNH.html
            Doctrine: *FC Choice* (Fits in MOTD)
            Shield ENIs > Scalpels > Dictors > Handout vigils
            
            ~~~ This was a coord broadcast from epofhis to all at 2024-01-24 14:05:11.680333 EVE ~~~
        """.trimIndent() to PingModel.FleetPing(
            timestamp = timestamp,
            sourceText = "",
            description = "I found an absolutely amazing wh rout of awesome. Getin, im itching for some blood!",
            fleetCommander = FleetCommander("Mist Amatin", 3),
            fleet = null,
            formupLocations = listOf(FormupLocation.System("1DQ1-A")),
            papType = PapType.Strategic,
            comms = Comms.Mumble("Op 3", "https://gnf.lt/NOH1FNH.html"),
            doctrine = Doctrine(
                text = "*FC Choice* (Fits in MOTD)\nShield ENIs > Scalpels > Dictors > Handout vigils",
                link = "https://goonfleet.com/index.php/topic/349390-active-peacetime-eni-fleet/",
            ),
            broadcastSource = "coord",
            target = "all",
        ),
        """
            Fleet up on Asher

            FC: Asher Elias
            Fleet name: Asher's big time fun fleet #1 sir
            Comms: Op 1
            Sun Tzu quote: Optional

            ~~~ This was a broadcast from asher_elias to discord at 2024-01-31 00:17:02.533642 EVE ~~~
        """.trimIndent() to PingModel.FleetPing(
            timestamp = timestamp,
            sourceText = "",
            description = "Fleet up on Asher\n\nSun Tzu quote: Optional",
            fleetCommander = FleetCommander("Asher Elias", 4),
            fleet = "Asher's big time fun fleet #1 sir",
            formupLocations = emptyList(),
            papType = null,
            comms = Comms.Text("Op 1"),
            doctrine = null,
            broadcastSource = null,
            target = "discord",
        ),
        """
            Fleet up on Asher
            
            FC: Asher Elias
            Formup Location: U-Q
            
            ~~~ This was a broadcast from asher_elias to discord at 2024-01-31 00:17:02.533642 EVE ~~~
        """.trimIndent() to PingModel.FleetPing(
            timestamp = timestamp,
            sourceText = "",
            description = "Fleet up on Asher",
            fleetCommander = FleetCommander("Asher Elias", 4),
            fleet = null,
            formupLocations = listOf(FormupLocation.System("U-QMOA")),
            papType = null,
            comms = null,
            doctrine = null,
            broadcastSource = null,
            target = "discord",
        ),
    ).forEachIndexed { index, (text, expected) ->
        "ping $index is parsed correctly" {
            val actual = target(timestamp, text)

            actual.shouldNotBeNull()
            actual.shouldBeEqualToComparingFields(
                other = expected,
                fieldsEqualityCheckConfig = FieldsEqualityCheckConfig(
                    propertiesToExclude = listOf(
                        PingModel.FleetPing::sourceText,
                        PingModel.PlainText::sourceText,
                    ),
                ),
            )
        }
    }
})
