package dev.nohus.rift.database

import dev.nohus.rift.database.static.StaticDatabase
import dev.nohus.rift.repositories.ShipTypesRepository
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe

class ShipTypesRepositoryTest : FreeSpec({

    val target = ShipTypesRepository(StaticDatabase())

    listOf(
        "Buzzard" to "Buzzard",
        "buzzard" to "Buzzard",
        "buZZaRD" to "Buzzard",
        "Caldari Shuttle" to "Caldari Shuttle",
        "caldari shuttle" to "Caldari Shuttle",
        "kiki" to "Kikimora",
        "execuror" to "Exequror",
        "Exequror Navy" to "Exequror Navy Issue",
        "execuror navy" to "Exequror Navy Issue",
        "Exequror Navy Issue" to "Exequror Navy Issue",
        "Augoror Navy Issue" to "Augoror Navy Issue",
        "navy drake" to "Drake Navy Issue",
        "navy comet" to "Federation Navy Comet",
        "fleet cyclone" to "Cyclone Fleet Issue",
        "Auguror Navy Issue" to null,
        "cyclone fleet" to null,
        "invalid" to null,
        "" to null,
        "Buzzar" to null,
        " Buzzard" to null,
        "Buzzard " to null,
    ).forEach { (input, expected) ->
        "for input \"$input\", getShip() returns \"$expected\"" {
            val actual = target.getShip(input)
            actual shouldBe expected
        }
    }
})
