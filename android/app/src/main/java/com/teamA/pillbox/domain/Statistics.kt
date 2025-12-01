package com.teamA.pillbox.domain

/**
 * Statistics about medication adherence over a date range.
 */
data class Statistics(
    /**
     * Start date of the statistics period.
     */
    val startDate: java.time.LocalDate,
    
    /**
     * End date of the statistics period.
     */
    val endDate: java.time.LocalDate,
    
    /**
     * Total number of scheduled doses in this period.
     */
    val totalScheduled: Int,
    
    /**
     * Number of doses that were taken.
     */
    val totalTaken: Int,
    
    /**
     * Number of doses that were missed.
     */
    val totalMissed: Int,
    
    /**
     * Number of doses still pending (scheduled but not yet due).
     */
    val totalPending: Int,
    
    /**
     * Compliance percentage (taken / (taken + missed) * 100).
     * Only counts completed doses (taken or missed), excludes pending.
     * Range: 0.0 to 100.0
     */
    val compliancePercentage: Double,
    
    /**
     * Current streak of consecutive days with medication taken.
     * 0 if no streak exists.
     */
    val currentStreak: Int
) {
    init {
        require(totalScheduled >= 0) { "Total scheduled must be non-negative" }
        require(totalTaken >= 0) { "Total taken must be non-negative" }
        require(totalMissed >= 0) { "Total missed must be non-negative" }
        require(totalPending >= 0) { "Total pending must be non-negative" }
        require(totalScheduled == totalTaken + totalMissed + totalPending) {
            "Total scheduled must equal sum of taken, missed, and pending"
        }
        require(compliancePercentage in 0.0..100.0) {
            "Compliance percentage must be between 0 and 100"
        }
        require(currentStreak >= 0) { "Current streak must be non-negative" }
    }
    
    /**
     * Calculate compliance percentage from taken and missed counts.
     */
    companion object {
        fun calculateCompliance(taken: Int, missed: Int): Double {
            val total = taken + missed
            return if (total > 0) {
                (taken.toDouble() / total) * 100.0
            } else {
                0.0
            }
        }
    }
}

