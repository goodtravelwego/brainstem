package com.brainstem.stimulate

import android.content.Context
import androidx.core.content.edit
import org.json.JSONArray
import org.json.JSONObject

fun JSONArray.objects() = (0 until length()).map { getJSONObject(it) }

fun JSONArray.strings() = (0 until length()).map { getString(it) }

fun Step.json() =
    JSONObject()
        .put("label", label)
        .put("pts", JSONArray(pts))
        .put("mode", mode)
        .put("f", f)
        .put("f1", f1)
        .put("f2", f2)
        .put("dur", dur)
        .put("P", power)
        .put("duty", duty)

fun stepFrom(j: JSONObject) =
    Step(
        j.optString("label", "Step"),
        j.optJSONArray("pts")?.strings() ?: POINTS,
        j.optString("mode", "fixed"),
        j.optDouble("f", 10.0),
        j.optDouble("f1", 8.0),
        j.optDouble("f2", 12.0),
        j.optInt("dur", 600),
        j.optInt("P", 100),
        j.optInt("duty", 0),
    )

fun Program.json() =
    JSONObject()
        .put("id", id)
        .put("name", name)
        .put("cat", cat)
        .put("ic", icon)
        .put("goal", goal)
        .put("note", note)
        .put("src", src)
        .put("fixedHz", fixedHz)
        .put("steps", JSONArray(steps.map { it.json() }))

fun programFrom(j: JSONObject) =
    Program(
        id = j.getString("id"),
        name = j.getString("name"),
        cat = j.optString("cat", "custom"),
        icon = j.optString("ic", "sun"),
        goal = j.optString("goal", "สร้างเอง"),
        note = j.optString("note"),
        src = j.optString("src", "custom"),
        fixedHz = j.optBoolean("fixedHz") || j.optInt("fixedHz") == 1,
        steps = j.getJSONArray("steps").objects().map(::stepFrom),
    )

fun Profile.json() =
    JSONObject()
        .put("id", id)
        .put("name", name)
        .put("age", age)
        .put("cloud", cloud)
        .put("real", realName)
        .put("sex", sex)
        .put("birth", birth)
        .put("type", type)
        .put("hand", hand)
        .put("guardianName", guardianName)
        .put("guardianRelation", guardianRelation)
        .put("guardianPhone", guardianPhone)
        .put("photo", photo)
        .put("photoPath", photoPath)

fun profileFrom(j: JSONObject) =
    Profile(
        j.getString("id"),
        j.getString("name"),
        j.optInt("age", 9),
        j.optString("cloud"),
        j.optString("real", j.getString("name")),
        j.optString("sex"),
        j.optString("birth"),
        j.optString("type", if (j.optInt("age", 9) < 18) "kid" else "adult"),
        j.optString("hand"),
        j.optString("guardianName"),
        j.optString("guardianRelation"),
        j.optString("guardianPhone"),
        j.optString("photo"),
        j.optString("photoPath"),
    )

fun SessionRecord.json() =
    JSONObject()
        .put("date", date)
        .put("complete", complete)
        .put("sec", seconds)
        .put("lvl", level)
        .put("dose", JSONObject(dose))
        .put("cl", closedLoop)
        .put("trace", JSONArray(alphaTrace))
        .put("deltas", JSONArray(deltas))
        .put("iapf", iapf)
        .put("initialLevel", initialLevel)
        .put("events", JSONArray(events))
        .put(
            "items",
            JSONArray(
                items.map {
                    JSONObject()
                        .put("pid", it.pid)
                        .put("name", it.name)
                        .put("min", it.minutes)
                        .put("sec", it.seconds)
                }
            ),
        )

fun recordFrom(j: JSONObject) =
    SessionRecord(
        date = j.getLong("date"),
        complete = j.getBoolean("complete"),
        seconds = j.getInt("sec"),
        level = j.optInt("lvl", 1),
        iapf = j.optDouble("iapf", 0.0),
        initialLevel = j.optInt("initialLevel", j.optInt("lvl", 1)),
        events = j.optJSONArray("events")?.strings() ?: emptyList(),
        dose =
            j.getJSONObject("dose").let { d ->
                d.keys().asSequence().associateWith { d.getDouble(it) }
            },
        closedLoop = j.optBoolean("cl"),
        alphaTrace =
            j.optJSONArray("trace")?.let { a -> (0 until a.length()).map { a.getDouble(it) } }
                ?: emptyList(),
        deltas =
            j.optJSONArray("deltas")?.let { a -> (0 until a.length()).map { a.getDouble(it) } }
                ?: emptyList(),
        items =
            j.getJSONArray("items").objects().map {
                SessionItem(
                    it.getString("pid"),
                    it.getString("name"),
                    it.getInt("min"),
                    it.getInt("sec"),
                )
            },
    )

class Repository(context: Context) {
    val prefs = context.getSharedPreferences("stimulate", Context.MODE_PRIVATE)
    val definitions =
        JSONObject(context.assets.open("programs.json").bufferedReader().use { it.readText() })
    val presets = definitions.getJSONArray("programs").objects().map(::programFrom)
    val info = definitions.getJSONObject("info")
    val references = definitions.getJSONObject("references")
    val evidence = definitions.getJSONObject("evidence")

    fun regionName(points: List<String>): String {
        val regions = definitions.getJSONObject("regions")
        val match =
            regions
                .keys()
                .asSequence()
                .map { regions.getJSONArray(it) }
                .firstOrNull { it.getJSONArray(1).strings().toSet() == points.toSet() }
        return match?.getString(0) ?: "${points.size} จุด"
    }

    fun regionCaption(points: List<String>): String {
        val name = regionName(points)
        return if ("จุด" in name) name else "$name · ${points.size} จุด"
    }

    fun read(key: String) =
        runCatching { JSONObject(prefs.getString(key, "{}")!!) }.getOrDefault(JSONObject())

    fun array(key: String) =
        runCatching { JSONArray(prefs.getString(key, "[]")!!) }.getOrDefault(JSONArray())

    fun put(key: String, value: Any) {
        prefs.edit { putString(key, value.toString()) }
    }
}
