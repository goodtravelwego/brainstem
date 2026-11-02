package com.brainstem.stimulate

import android.content.Context
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

fun profileFrom(j: JSONObject) =
    Profile(
        j.getString("id"),
        j.getString("name"),
        j.optInt("age", 9),
        j.optString("cloud"),
        j.optString("real", j.getString("name")),
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

    fun read(key: String) =
        runCatching { JSONObject(prefs.getString(key, "{}")!!) }.getOrDefault(JSONObject())

    fun array(key: String) =
        runCatching { JSONArray(prefs.getString(key, "[]")!!) }.getOrDefault(JSONArray())

    fun put(key: String, value: Any) {
        prefs.edit().putString(key, value.toString()).apply()
    }
}
