package app.planlift.fit.domain

/**
 * 同步合并：同一条记录（同 id）比 rev，rev 大者胜；rev 相同比 updatedAt；
 * 再相同用 deviceId 字典序决定（保证两台设备算出同一个结果）。
 *
 * `deleted` 只是记录上的字段，参与同样的比较——所以删除也能传播。
 */
data class MergeOutcome<T>(
    val merged: List<T>,
    val localWins: Int,
    val remoteWins: Int,
    val sameRecord: Int
)

object SyncMerge {

    fun <T> merge(
        local: List<T>,
        remote: List<T>,
        key: (T) -> String,
        rev: (T) -> Long,
        updatedAt: (T) -> Long,
        deviceId: (T) -> String
    ): MergeOutcome<T> {
        val localMap = local.associateBy(key)
        val remoteMap = remote.associateBy(key)
        val allKeys = LinkedHashSet<String>().apply {
            addAll(localMap.keys)
            addAll(remoteMap.keys)
        }

        var localWins = 0
        var remoteWins = 0
        var same = 0
        val merged = ArrayList<T>(allKeys.size)

        for (k in allKeys) {
            val l = localMap[k]
            val r = remoteMap[k]
            when {
                l == null && r != null -> {
                    merged += r
                    remoteWins++
                }
                l != null && r == null -> {
                    merged += l
                    localWins++
                }
                l != null && r != null -> {
                    val cmp = compare(l, r, rev, updatedAt, deviceId)
                    if (cmp >= 0) {
                        merged += l
                        if (cmp > 0) localWins++ else same++
                    } else {
                        merged += r
                        remoteWins++
                    }
                }
            }
        }
        return MergeOutcome(merged, localWins, remoteWins, same)
    }

    private fun <T> compare(
        a: T,
        b: T,
        rev: (T) -> Long,
        updatedAt: (T) -> Long,
        deviceId: (T) -> String
    ): Int {
        val byRev = rev(a).compareTo(rev(b))
        if (byRev != 0) return byRev
        val byTime = updatedAt(a).compareTo(updatedAt(b))
        if (byTime != 0) return byTime
        return deviceId(a).compareTo(deviceId(b))
    }
}
