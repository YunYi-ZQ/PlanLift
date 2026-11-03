package app.planlift.fit.data.export

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import app.planlift.fit.data.local.EnrollmentEntity
import app.planlift.fit.data.local.SetLogEntity
import app.planlift.fit.data.local.SessionEntity
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object Exporter {

    private fun dir(context: Context): File =
        File(context.getExternalFilesDir(null) ?: context.filesDir, "exports").apply { mkdirs() }

    private fun stamp(): String =
        SimpleDateFormat("yyyyMMdd-HHmm", Locale.US).format(Date())

    /** 全量 JSON：计划进度 + 训练 + 每组 + 围度。W4 的 WebDAV 快照可直接复用这个结构。 */
    fun exportJson(
        context: Context,
        enrollment: EnrollmentEntity?,
        sessions: List<SessionEntity>,
        sets: Map<String, List<SetLogEntity>>,
        metrics: List<Pair<Long, Map<String, Double?>>>
    ): File {
        val root = buildJsonObject {
            put("schema", 1)
            put("exportedAt", System.currentTimeMillis())
            enrollment?.let { e ->
                put("enrollment", buildJsonObject {
                    put("id", e.id)
                    put("planId", e.planId)
                    put("startDate", e.startDate)
                    put("currentWeek", e.currentWeek)
                    put("status", e.status)
                    put("rev", e.rev)
                })
            }
            put("sessions", buildJsonArray {
                sessions.forEach { s ->
                    add(buildJsonObject {
                        put("id", s.id)
                        put("planDayIndex", s.planDayIndex)
                        put("dayNameZh", s.dayNameZh)
                        put("dayNameEn", s.dayNameEn)
                        put("date", s.date)
                        put("durationMin", s.durationMin)
                        put("totalSets", s.totalSets)
                        put("totalVolumeKg", s.totalVolumeKg)
                        put("sets", buildJsonArray {
                            (sets[s.id] ?: emptyList()).forEach { l ->
                                add(buildJsonObject {
                                    put("exerciseId", l.exerciseId)
                                    put("exerciseZh", l.exerciseZh)
                                    put("weightKg", l.weightKg)
                                    put("reps", l.reps)
                                    put("rir", l.rir)
                                })
                            }
                        })
                    })
                }
            })
            put("metrics", buildJsonArray {
                metrics.forEach { (date, values) ->
                    add(buildJsonObject {
                        put("date", date)
                        values.forEach { (k, v) -> if (v != null) put(k, v) }
                    })
                }
            })
        }
        val file = File(dir(context), "planlift-${stamp()}.json")
        file.writeText(Json { prettyPrint = true }.encodeToString(kotlinx.serialization.json.JsonObject.serializer(), root))
        return file
    }

    /** 逐组 CSV，方便丢进 Excel 画图 */
    fun exportCsv(context: Context, sessions: List<SessionEntity>, sets: Map<String, List<SetLogEntity>>): File {
        val sb = StringBuilder("date,session,day,exercise,set,weight_kg,reps,rir\n")
        val fmt = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US)
        sessions.forEach { s ->
            (sets[s.id] ?: emptyList()).forEachIndexed { i, l ->
                sb.append(fmt.format(Date(s.date))).append(',')
                    .append(s.id.take(8)).append(',')
                    .append(escape(s.dayNameEn)).append(',')
                    .append(escape(l.exerciseEn)).append(',')
                    .append(i + 1).append(',')
                    .append(l.weightKg).append(',')
                    .append(l.reps).append(',')
                    .append(l.rir).append('\n')
            }
        }
        val file = File(dir(context), "planlift-${stamp()}.csv")
        file.writeText(sb.toString())
        return file
    }

    private fun escape(s: String): String =
        if (s.contains(',') || s.contains('"')) "\"" + s.replace("\"", "\"\"") + "\"" else s

    fun share(context: Context, file: File) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = if (file.extension == "csv") "text/csv" else "application/json"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, file.name).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }
}
