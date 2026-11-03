package app.planlift.fit.domain

/**
 * 双重递进（Double Progression）+ RIR 的加重引擎。
 *
 * 输入：上一次同一动作的实际表现、目标次数区间、当前重量、器械/部位对应的最小加重档。
 * 输出：下一次该用的重量与目标次数，以及原因（用于 UI 解释「为什么给你这个重量」）。
 *
 * 纯函数，无 Android 依赖，可直接单元测试。
 */

data class SetPerformance(
    val weightKg: Double,
    val reps: Int,
    val rir: Int
)

data class ProgressionTarget(
    val repMin: Int,
    val repMax: Int,
    val rir: Int,
    val plannedSets: Int
)

enum class ProgressionAction {
    /** 第一次练 / 没有历史，直接给起始重量 */
    START,
    /** 所有组达到次数上限且还有余力 → 加一档，次数回到下限 */
    UP,
    /** 没到上限但也没崩 → 保持重量，下次目标多 1 次 */
    HOLD,
    /** 有组数低于下限 → 降半档，避免崩盘 */
    DOWN
}

data class ProgressionResult(
    val nextWeightKg: Double,
    val nextReps: Int,
    val action: ProgressionAction,
    val reason: String
)

object ProgressionEngine {

    private const val EPS = 0.001

    /**
     * @param last 上一次该动作的所有正式组（按顺序），空表示首次
     * @param target 目标次数区间与 RIR
     * @param currentWeight 上次使用的重量（无历史时作为起始参考）
     * @param increment 一档加重（kg），由部位与性别决定
     */
    fun next(
        last: List<SetPerformance>,
        target: ProgressionTarget,
        currentWeight: Double,
        increment: Double
    ): ProgressionResult {
        val inc = if (increment <= 0.0) 2.5 else increment
        val startWeight = if (currentWeight > 0.0) currentWeight else 0.0

        if (last.isEmpty()) {
            return ProgressionResult(
                nextWeightKg = startWeight,
                nextReps = target.repMin,
                action = ProgressionAction.START,
                reason = "首次训练：从 $startWeight kg 开始，先做到 ${target.repMin} 次"
            )
        }

        val valid = last.filter { it.reps > 0 && it.weightKg > 0.0 }
        if (valid.isEmpty()) {
            return ProgressionResult(
                nextWeightKg = startWeight,
                nextReps = target.repMin,
                action = ProgressionAction.START,
                reason = "上次没有有效记录：重新从 $startWeight kg 开始"
            )
        }

        val weight = valid.maxOf { it.weightKg }
        val minReps = valid.minOf { it.reps }
        val maxReps = valid.maxOf { it.reps }
        val minRir = valid.minOf { it.rir }

        // 1) 全部组都达到次数上限，且没有练到力竭（RIR 仍 >= 目标）→ 加一档
        if (minReps >= target.repMax && minRir >= target.rir) {
            val next = weight + inc
            return ProgressionResult(
                nextWeightKg = next,
                nextReps = target.repMin,
                action = ProgressionAction.UP,
                reason = "全部组达到 ${target.repMax} 次且 RIR $minRir：加 ${fmt(inc)} kg 到 ${fmt(next)} kg，次数回到 ${target.repMin}"
            )
        }

        // 2) 有组低于次数下限 → 降半档
        if (minReps < target.repMin) {
            val next = maxOf(0.0, weight - inc / 2.0)
            return ProgressionResult(
                nextWeightKg = next,
                nextReps = target.repMin,
                action = ProgressionAction.DOWN,
                reason = "有组只做到 $minReps 次（低于 ${
                    target.repMin
                } 次）：降到 ${fmt(next)} kg 重新累积"
            )
        }

        // 3) 其他情况：重量不变，目标次数 +1（不超过上限）
        val nextReps = (maxReps + 1).coerceAtMost(target.repMax)
        return ProgressionResult(
            nextWeightKg = weight,
            nextReps = nextReps,
            action = ProgressionAction.HOLD,
            reason = "重量保持 ${fmt(weight)} kg，下次目标 $nextReps 次"
        )
    }

    /**
     * 一档加重规则：
     * 下肢 5 kg（女性 2.5），上肢 2.5 kg（女性 1.25），器械/绳索取更小档。
     */
    fun increment(lowerBody: Boolean, isMachine: Boolean, sex: String): Double {
        val female = sex.equals("female", ignoreCase = true)
        return when {
            lowerBody && female -> 2.5
            lowerBody -> 5.0
            isMachine -> 2.5
            female -> 1.25
            else -> 2.5
        }
    }

    /** Epley 公式估算 1RM；次数为 1 时直接返回重量。 */
    fun e1rm(weightKg: Double, reps: Int): Double =
        if (reps <= 0 || weightKg <= 0.0) 0.0
        else if (reps == 1) weightKg
        else weightKg * (1.0 + reps / 30.0)

    private fun fmt(v: Double): String =
        if (kotlin.math.abs(v - v.toInt()) < EPS) v.toInt().toString() else v.toString()
}
