package com.blocksocial.lite.detection

data class DecisionRecord(
    val atMillis: Long,
    val app: String?,
    val transition: TransitionOutcome,
    val usedMinutes: Int?,
    val limitMinutes: Int?,
    val warned: Boolean,
)

class DecisionLog(private val capacity: Int = CAPACITY) {

    private val records = ArrayDeque<DecisionRecord>()

    @Synchronized
    fun record(record: DecisionRecord) {
        records.addLast(record)
        while (records.size > capacity) records.removeFirst()
    }

    @Synchronized
    fun newestFirst(): List<DecisionRecord> = records.reversed()

    @Synchronized
    fun clear() = records.clear()

    private companion object {
        const val CAPACITY = 30
    }
}
