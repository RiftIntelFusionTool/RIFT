package dev.nohus.rift.planetaryindustry.simulation

import java.time.Duration
import java.time.Instant
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.pow

class ExtractionSimulation {
    companion object {
        private const val SEC = 10000000L

        fun getProgramOutputPrediction(
            baseValue: Int,
            cycleDuration: Duration,
            length: Int,
        ): List<Long> {
            val vals = mutableListOf<Long>()
            val startTime = 0L
            val cycleTime = cycleDuration.toSeconds() * SEC
            for (i in 0 until length) {
                val currentTime = (i + 1) * cycleTime
                vals += getProgramOutput(baseValue, startTime, currentTime, cycleTime)
            }
            return vals
        }

        fun getProgramOutput(
            baseValue: Int,
            startTime: Instant,
            currentTime: Instant,
            cycleTime: Duration,
        ): Long {
            return getProgramOutput(
                baseValue = baseValue,
                startTime = startTime.epochSecond * SEC,
                currentTime = currentTime.epochSecond * SEC,
                cycleTime = cycleTime.toSeconds() * SEC,
            )
        }

        fun getProgramOutput(
            baseValue: Int,
            startTime: Long,
            currentTime: Long,
            cycleTime: Long,
        ): Long {
            val decayFactor = 0.012
            val noiseFactor = 0.8
            val timeDiff = currentTime - startTime
            val cycleNum = max((timeDiff + SEC) / cycleTime - 1, 0)
            val barWidth = cycleTime / SEC / 900.0
            val t = (cycleNum + 0.5) * barWidth
            val decayValue = baseValue / (1 + t * decayFactor)
            val f1 = 1.0 / 12.0
            val f2 = 1.0 / 5.0
            val f3 = 1.0 / 2.0
            val phaseShift = baseValue.toDouble().pow(0.7)
            val sinA = cos(phaseShift + t * f1)
            val sinB = cos(phaseShift / 2.0 + t * f2)
            val sinC = cos(t * f3)
            var sinStuff = (sinA + sinB + sinC) / 3.0
            sinStuff = max(0.0, sinStuff)
            val barHeight = decayValue * (1 + noiseFactor * sinStuff)

            val output = barWidth * barHeight
            // Round down, with integers also rounded down (123.0 -> 122)
            return if (output - output.toLong() == 0.0) output.toLong() - 1 else output.toLong()
        }
    }
}
