package com.example.strongholdcalculator

import kotlin.math.*

data class StrongholdCell(
    var centerX: Double = 0.0,
    var centerZ: Double = 0.0,
    var xMin: Double = 0.0,
    var xMax: Double = 0.0,
    var zMin: Double = 0.0,
    var zMax: Double = 0.0,
    var prob: Double = 0.0,
    var distance: Double = 0.0,
    var distanceRange: Int = 0
)

data class StrongholdCandidate(
    var projectionX: Int = 0,
    var projectionZ: Int = 0,
    var netherX: Int = 0,
    var netherZ: Int = 0,
    var cellCenterX: Double = 0.0,
    var cellCenterZ: Double = 0.0,
    var rawProb: Double = 0.0,
    var conditionalProb: Double = 0.0,
    var distance: Int = 0,
    var distanceFromOrigin: Int = 0,
    var distanceRange: Int = 0,
    var bounds: String = ""
)

data class TriangulationResult(
    val candidates: List<StrongholdCandidate>,
    val strongholdX: Double,
    val strongholdZ: Double,
    val uncertainty: Double
)

class StrongholdCalculator {
    companion object {
        private val distanceProbabilities = mapOf(
            500 to 0.0262, 600 to 0.0639, 800 to 0.1705, 900 to 0.1582, 1000 to 0.1427,
            1100 to 0.1204, 1200 to 0.0919, 1300 to 0.1133, 1400 to 0.1139, 1500 to 0.1228,
            1700 to 0.0586, 1800 to 0.0535, 1900 to 0.0610, 2100 to 0.0590, 2200 to 0.0431,
            2300 to 0.0375, 2400 to 0.0292, 2500 to 0.0493, 2600 to 0.0382, 2700 to 0.0347,
            2800 to 0.0258, 3000 to 0.0171, 3100 to 0.0169, 3200 to 0.0189
        )
        private const val COORDINATE_STD_DEV = 50.0 // Standard deviation for coordinate-based method
        private const val DISTANCE_STD_DEV = 25.0   // Standard deviation for distance-based method
    }

    private val strongholdCells = mutableListOf<StrongholdCell>()

    fun generateStrongholdCells() {
        strongholdCells.clear()
        val cellSize = 272
        val gap = 160
        val totalStep = cellSize + gap

        for (xIndex in -15..15) {
            for (zIndex in -15..15) {
                val xMin: Double
                val xMax: Double
                val zMin: Double
                val zMax: Double

                if (xIndex >= 0) {
                    xMin = (xIndex * totalStep).toDouble()
                    xMax = xMin + cellSize
                } else {
                    xMax = (xIndex * totalStep - gap).toDouble()
                    xMin = xMax - cellSize
                }

                if (zIndex >= 0) {
                    zMin = (zIndex * totalStep).toDouble()
                    zMax = zMin + cellSize
                } else {
                    zMax = (zIndex * totalStep - gap).toDouble()
                    zMin = zMax - cellSize
                }

                val centerX = (xMin + xMax) / 2.0
                val centerZ = (zMin + zMax) / 2.0
                val distanceFromOrigin = sqrt(centerX * centerX + centerZ * centerZ)

                if (distanceFromOrigin >= 512) {
                    var closestDistance = 500
                    var minDiff = abs(500 - distanceFromOrigin.toInt())

                    for ((distance, _) in distanceProbabilities) {
                        val diff = abs(distance - distanceFromOrigin.toInt())
                        if (diff < minDiff) {
                            minDiff = diff
                            closestDistance = distance
                        }
                    }

                    val prob = distanceProbabilities[closestDistance] ?: 0.0
                    strongholdCells.add(StrongholdCell(
                        centerX = centerX, centerZ = centerZ,
                        xMin = xMin, xMax = xMax, zMin = zMin, zMax = zMax,
                        prob = prob, distance = distanceFromOrigin, distanceRange = closestDistance
                    ))
                }
            }
        }
    }

    /**
     * Calculate stronghold location using only two coordinates
     * The stronghold lies on the vector from first to second coordinate
     */
    fun calculateStrongholdFromCoordinates(
        x1: Double, z1: Double,
        x2: Double, z2: Double
    ): TriangulationResult {
        val candidates = mutableListOf<StrongholdCandidate>()

        // Calculate the direction vector from first to second position
        val dx = x2 - x1
        val dz = z2 - z1
        val vectorLength = sqrt(dx * dx + dz * dz)

        if (vectorLength < 1e-10) {
            return TriangulationResult(emptyList(), x2, z2, 0.0)
        }

        // Normalize the direction vector
        val dirX = dx / vectorLength
        val dirZ = dz / vectorLength

        // Eye of ender starts flying from (x2 + 0.5, z2 + 0.5) - Bedrock fix
        val eyeStartX = x2 + 0.5
        val eyeStartZ = z2 + 0.5

        val cellProbabilities = mutableMapOf<StrongholdCell, Double>()
        val cellProjections = mutableMapOf<StrongholdCell, MutableList<Pair<Double, Double>>>()

        // Generate samples along the vector with uncertainty
        val numSamples = 20
        for (i in 0 until numSamples) {
            // Sample different distances along the vector
            val baseDistance = 500.0 + i * 200.0 // Test distances from 500 to 4300 blocks

            // Add uncertainty perpendicular to the main vector
            for (perpOffset in listOf(-COORDINATE_STD_DEV, 0.0, COORDINATE_STD_DEV)) {
                // Calculate perpendicular direction
                val perpX = -dirZ // Perpendicular to direction vector
                val perpZ = dirX

                // Calculate test point
                val testX = eyeStartX + baseDistance * dirX + perpOffset * perpX
                val testZ = eyeStartZ + baseDistance * dirZ + perpOffset * perpZ

                // Weight based on perpendicular offset (Gaussian)
                val perpWeight = gaussianProbability(perpOffset, 0.0, COORDINATE_STD_DEV)

                // Check intersection with stronghold cells
                for (cell in strongholdCells) {
                    // Check if test point is within or near this cell
                    val clampedX = maxOf(cell.xMin, minOf(cell.xMax, testX))
                    val clampedZ = maxOf(cell.zMin, minOf(cell.zMax, testZ))

                    val distanceToCell = sqrt(
                        (clampedX - testX).pow(2) + (clampedZ - testZ).pow(2)
                    )

                    // Allow up to 50 blocks deviation from the vector
                    if (distanceToCell <= COORDINATE_STD_DEV) {
                        val cellWeight = perpWeight * cell.prob *
                                gaussianProbability(distanceToCell, 0.0, COORDINATE_STD_DEV)

                        cellProbabilities[cell] = (cellProbabilities[cell] ?: 0.0) + cellWeight

                        if (cellProjections[cell] == null) {
                            cellProjections[cell] = mutableListOf()
                        }
                        cellProjections[cell]?.add(Pair(clampedX, clampedZ))
                    }
                }
            }
        }

        // Convert to candidates
        for ((cell, accumulatedProb) in cellProbabilities) {
            if (accumulatedProb > 0 && cellProjections[cell]?.isNotEmpty() == true) {
                val projections = cellProjections[cell]!!
                var avgX = 0.0
                var avgZ = 0.0
                for ((projX, projZ) in projections) {
                    avgX += projX
                    avgZ += projZ
                }
                avgX /= projections.size
                avgZ /= projections.size

                val distanceToProjection = sqrt((avgX - x2).pow(2) + (avgZ - z2).pow(2))

                candidates.add(StrongholdCandidate(
                    projectionX = avgX.roundToInt(), projectionZ = avgZ.roundToInt(),
                    netherX = (avgX / 8.0).roundToInt(), netherZ = (avgZ / 8.0).roundToInt(),
                    cellCenterX = cell.centerX, cellCenterZ = cell.centerZ,
                    rawProb = accumulatedProb, distance = distanceToProjection.roundToInt(),
                    distanceFromOrigin = cell.distance.roundToInt(), distanceRange = cell.distanceRange,
                    bounds = "(${cell.xMin.toInt()}, ${cell.zMin.toInt()}) to (${cell.xMax.toInt()}, ${cell.zMax.toInt()})"
                ))
            }
        }

        // Calculate conditional probabilities
        val totalRawProb = candidates.sumOf { it.rawProb }
        if (totalRawProb > 0) {
            for (candidate in candidates) {
                candidate.conditionalProb = candidate.rawProb / totalRawProb
            }
        }

        // Sort by conditional probability
        val sortedCandidates = candidates.sortedByDescending { it.conditionalProb }

        val bestCandidate = sortedCandidates.firstOrNull()
        val strongholdX = bestCandidate?.projectionX?.toDouble() ?: x2
        val strongholdZ = bestCandidate?.projectionZ?.toDouble() ?: z2

        return TriangulationResult(
            candidates = sortedCandidates,
            strongholdX = strongholdX,
            strongholdZ = strongholdZ,
            uncertainty = calculateUncertainty(sortedCandidates)
        )
    }

    /**
     * Calculate stronghold location using distance from pixel change
     */
    fun calculateStrongholdWithDistance(
        fromX: Double, fromZ: Double,
        towardX: Double, towardZ: Double,
        distance: Double
    ): TriangulationResult {
        val candidates = mutableListOf<StrongholdCandidate>()

        // Calculate direction from first to second position
        val dx = towardX - fromX
        val dz = towardZ - fromZ
        val vectorLength = sqrt(dx * dx + dz * dz)

        if (vectorLength < 1e-10) {
            return TriangulationResult(emptyList(), fromX, fromZ, 0.0)
        }

        // Normalize direction
        val dirX = dx / vectorLength
        val dirZ = dz / vectorLength

        // Eye of ender starts from (fromX + 0.5, fromZ + 0.5) - Bedrock fix
        val eyeStartX = fromX + 0.5
        val eyeStartZ = fromZ + 0.5

        val cellProbabilities = mutableMapOf<StrongholdCell, Double>()
        val cellProjections = mutableMapOf<StrongholdCell, MutableList<Pair<Double, Double>>>()

        // Generate distance samples with uncertainty
        val distanceSamples = generateDistanceSamples(distance)

        for (distanceTest in distanceSamples) {
            val distanceWeight = gaussianProbability(distanceTest, distance, DISTANCE_STD_DEV)

            // Calculate exact point at this distance
            val exactX = eyeStartX + distanceTest * dirX
            val exactZ = eyeStartZ + distanceTest * dirZ

            // Check all cells for intersection or proximity
            for (cell in strongholdCells) {
                val clampedX = maxOf(cell.xMin, minOf(cell.xMax, exactX))
                val clampedZ = maxOf(cell.zMin, minOf(cell.zMax, exactZ))

                val distanceToCell = sqrt(
                    (clampedX - exactX).pow(2) + (clampedZ - exactZ).pow(2)
                )

                // Allow up to 50 blocks deviation
                if (distanceToCell <= COORDINATE_STD_DEV) {
                    val cellWeight = distanceWeight * cell.prob *
                            gaussianProbability(distanceToCell, 0.0, COORDINATE_STD_DEV)

                    cellProbabilities[cell] = (cellProbabilities[cell] ?: 0.0) + cellWeight

                    if (cellProjections[cell] == null) {
                        cellProjections[cell] = mutableListOf()
                    }
                    cellProjections[cell]?.add(Pair(clampedX, clampedZ))
                }
            }
        }

        // Convert to candidates
        for ((cell, accumulatedProb) in cellProbabilities) {
            if (accumulatedProb > 0 && cellProjections[cell]?.isNotEmpty() == true) {
                val projections = cellProjections[cell]!!
                var avgX = 0.0
                var avgZ = 0.0
                for ((projX, projZ) in projections) {
                    avgX += projX
                    avgZ += projZ
                }
                avgX /= projections.size
                avgZ /= projections.size

                val distanceToProjection = sqrt((avgX - fromX).pow(2) + (avgZ - fromZ).pow(2))

                candidates.add(StrongholdCandidate(
                    projectionX = avgX.roundToInt(), projectionZ = avgZ.roundToInt(),
                    netherX = (avgX / 8.0).roundToInt(), netherZ = (avgZ / 8.0).roundToInt(),
                    cellCenterX = cell.centerX, cellCenterZ = cell.centerZ,
                    rawProb = accumulatedProb, distance = distanceToProjection.roundToInt(),
                    distanceFromOrigin = cell.distance.roundToInt(), distanceRange = cell.distanceRange,
                    bounds = "(${cell.xMin.toInt()}, ${cell.zMin.toInt()}) to (${cell.xMax.toInt()}, ${cell.zMax.toInt()})"
                ))
            }
        }

        // Calculate conditional probabilities
        val totalRawProb = candidates.sumOf { it.rawProb }
        if (totalRawProb > 0) {
            for (candidate in candidates) {
                candidate.conditionalProb = candidate.rawProb / totalRawProb
            }
        }

        // Sort by conditional probability
        val sortedCandidates = candidates.sortedByDescending { it.conditionalProb }

        val bestCandidate = sortedCandidates.firstOrNull()
        val strongholdX = bestCandidate?.projectionX?.toDouble() ?: (fromX + distance * dirX)
        val strongholdZ = bestCandidate?.projectionZ?.toDouble() ?: (fromZ + distance * dirZ)

        return TriangulationResult(
            candidates = sortedCandidates,
            strongholdX = strongholdX,
            strongholdZ = strongholdZ,
            uncertainty = calculateUncertainty(sortedCandidates)
        )
    }

    private fun generateDistanceSamples(centerDistance: Double, numSamples: Int = 5): List<Double> {
        val distances = mutableListOf<Double>()
        for (i in 0 until numSamples) {
            val offset = (i - numSamples / 2) * (DISTANCE_STD_DEV / 2.0)
            distances.add(maxOf(0.0, centerDistance + offset))
        }
        return distances
    }

    private fun gaussianProbability(x: Double, mean: Double, stdDev: Double): Double {
        if (stdDev <= 0) return 0.0
        val exponent = -0.5 * ((x - mean) / stdDev).pow(2)
        return exp(exponent) / (stdDev * sqrt(2.0 * PI))
    }

    private fun calculateUncertainty(candidates: List<StrongholdCandidate>): Double {
        if (candidates.size < 2) return 0.0
        val bestCandidate = candidates[0]
        var maxDistance = 0.0
        for (candidate in candidates.take(5)) {
            val distance = sqrt(
                (candidate.projectionX - bestCandidate.projectionX).toDouble().pow(2) +
                        (candidate.projectionZ - bestCandidate.projectionZ).toDouble().pow(2)
            )
            maxDistance = maxOf(maxDistance, distance)
        }
        return maxDistance
    }
}