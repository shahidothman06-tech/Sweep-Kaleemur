package com.hidden.devices.detector.bugs.finder.scan

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

enum class ReportType { SCAN, CHECKLIST }

data class Report(
    val time: Long,
    val type: ReportType,
    val high: Int = 0,
    val medium: Int = 0,
    val low: Int = 0,
    val checked: Int = 0,
    val total: Int = 0,
    val room: String? = null,
)

/** Small key-value store for settings, reports and checklist ticks. Everything stays on the phone. */
class Prefs(context: Context) {
    private val sp = context.applicationContext.getSharedPreferences("sweep", Context.MODE_PRIVATE)

    var languageChosen: Boolean
        get() = sp.getBoolean("language_chosen", false)
        set(v) = sp.edit().putBoolean("language_chosen", v).apply()

    var completedScans: Int
        get() = sp.getInt("completed_scans", 0)
        set(v) = sp.edit().putInt("completed_scans", v).apply()

    /** Set once the person answered the rate prompt either way. */
    var rateAnswered: Boolean
        get() = sp.getBoolean("rate_answered", false)
        set(v) = sp.edit().putBoolean("rate_answered", v).apply()

    var soundOn: Boolean
        get() = sp.getBoolean("sound_on", true)
        set(v) = sp.edit().putBoolean("sound_on", v).apply()

    var isProCached: Boolean
        get() = sp.getBoolean("is_pro", false)
        set(v) = sp.edit().putBoolean("is_pro", v).apply()

    var checkedSpots: Set<String>
        get() = sp.getStringSet("checked_spots", emptySet()) ?: emptySet()
        set(v) = sp.edit().putStringSet("checked_spots", v).apply()

    fun reports(): List<Report> = runCatching {
        val arr = JSONArray(sp.getString("reports", "[]"))
        (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)
            Report(
                time = o.getLong("time"),
                type = ReportType.valueOf(o.getString("type")),
                high = o.optInt("high"), medium = o.optInt("medium"), low = o.optInt("low"),
                checked = o.optInt("checked"), total = o.optInt("total"),
                room = if (o.has("room")) o.getString("room") else null,
            )
        }
    }.getOrDefault(emptyList())

    fun addReport(r: Report, keep: Int) {
        val all = (listOf(r) + reports()).take(keep)
        val arr = JSONArray()
        all.forEach {
            arr.put(
                JSONObject()
                    .put("time", it.time).put("type", it.type.name)
                    .put("high", it.high).put("medium", it.medium).put("low", it.low)
                    .put("checked", it.checked).put("total", it.total)
                    .apply { if (it.room != null) put("room", it.room) },
            )
        }
        sp.edit().putString("reports", arr.toString()).apply()
    }

    fun clearReports() = sp.edit().remove("reports").apply()
}
