package com.brainstem.stimulate

import android.content.Context
import android.net.Uri
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import androidx.core.content.edit
import androidx.core.net.toUri
import java.net.HttpURLConnection
import java.net.URL
import java.security.KeyStore
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

/** Only public backend configuration is bundled. Session credentials use Android Keystore. */
class Auth(private val context: Context) {
    private val cfg =
        JSONObject(context.assets.open("backend.json").bufferedReader().use { it.readText() })
    private val prefs = context.getSharedPreferences("account", Context.MODE_PRIVATE)
    private val alias = "brainstem.auth.v1"
    private val redirect = cfg.getString("redirect")
    private val emailRedirect =
        cfg.optString("emailRedirect", "https://brainstem-monitor.netlify.app/app/")
    var photoUploadFailed = false
        private set

    private fun key(): SecretKey {
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (store.getKey(alias, null) as? SecretKey)?.let {
            return it
        }
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
            .apply {
                init(
                    KeyGenParameterSpec.Builder(
                            alias,
                            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
                        )
                        .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                        .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                        .build()
                )
            }
            .generateKey()
    }

    private fun encryptedSave(session: JSONObject?) {
        if (session == null) {
            prefs.edit { remove("session") }
            return
        }
        val cipher =
            Cipher.getInstance("AES/GCM/NoPadding").apply { init(Cipher.ENCRYPT_MODE, key()) }
        val bytes = cipher.iv + cipher.doFinal(session.toString().toByteArray(Charsets.UTF_8))
        prefs.edit { putString("session", Base64.encodeToString(bytes, Base64.NO_WRAP)) }
    }

    private fun session(): JSONObject? =
        runCatching {
                val bytes =
                    Base64.decode(prefs.getString("session", null) ?: return null, Base64.NO_WRAP)
                val cipher =
                    Cipher.getInstance("AES/GCM/NoPadding").apply {
                        init(
                            Cipher.DECRYPT_MODE,
                            key(),
                            GCMParameterSpec(128, bytes.copyOfRange(0, 12)),
                        )
                    }
                JSONObject(
                    String(cipher.doFinal(bytes.copyOfRange(12, bytes.size)), Charsets.UTF_8)
                )
            }
            .getOrNull()

    fun hasSession() = session() != null

    fun accountId() = session()?.optJSONObject("user")?.optString("id") ?: "guest"

    fun accountEmail() = session()?.optJSONObject("user")?.optString("email").orEmpty()

    fun clear() {
        encryptedSave(null)
    }

    private fun call(
        path: String,
        method: String = "GET",
        body: JSONObject? = null,
        token: String? = null,
        headers: Map<String, String> = emptyMap(),
    ): String =
        String(
            request(
                path,
                method,
                body?.toString()?.toByteArray(Charsets.UTF_8),
                token,
                if (body == null) headers else mapOf("Content-Type" to "application/json") + headers,
            ),
            Charsets.UTF_8,
        )

    private fun request(
        path: String,
        method: String = "GET",
        body: ByteArray? = null,
        token: String? = null,
        headers: Map<String, String> = emptyMap(),
    ): ByteArray {
        val connection = URL(cfg.getString("url") + path).openConnection() as HttpURLConnection
        try {
            connection.requestMethod = method
            connection.connectTimeout = 15000
            connection.readTimeout = 15000
            connection.setRequestProperty("apikey", cfg.getString("key"))
            token?.let { connection.setRequestProperty("Authorization", "Bearer $it") }
            headers.forEach { (key, value) -> connection.setRequestProperty(key, value) }
            if (body != null) {
                connection.doOutput = true
                connection.outputStream.use { it.write(body) }
            }
            val code = connection.responseCode
            val text =
                (if (code in 200..299) connection.inputStream else connection.errorStream)?.use {
                    it.readBytes()
                } ?: byteArrayOf()
            if (code !in 200..299) {
                val e =
                    runCatching { JSONObject(String(text, Charsets.UTF_8)) }
                        .getOrDefault(JSONObject())
                throw IllegalStateException(
                    message(
                        e.optString(
                            "msg",
                            e.optString(
                                "message",
                                e.optString("error_description", e.optString("error", "HTTP $code")),
                            ),
                        )
                    )
                )
            }
            return text
        } finally {
            connection.disconnect()
        }
    }

    private fun save(j: JSONObject) {
        if (j.has("access_token")) {
            if (!j.has("expires_at"))
                j.put(
                    "expires_at",
                    System.currentTimeMillis() / 1000 + j.optLong("expires_in", 3600),
                )
            encryptedSave(j)
        }
    }

    private fun token(): String {
        var s = session() ?: error("เซสชันหมดอายุ · เข้าสู่ระบบใหม่")
        if (s.optLong("expires_at") - System.currentTimeMillis() / 1000 < 60) {
            s =
                JSONObject(
                    call(
                        "/auth/v1/token?grant_type=refresh_token",
                        "POST",
                        JSONObject().put("refresh_token", s.getString("refresh_token")),
                    )
                )
            save(s)
        }
        return s.getString("access_token")
    }

    suspend fun signIn(email: String, password: String) =
        withContext(Dispatchers.IO) {
            save(
                JSONObject(
                    call(
                        "/auth/v1/token?grant_type=password",
                        "POST",
                        JSONObject().put("email", email).put("password", password),
                    )
                )
            )
        }

    suspend fun signUp(email: String, password: String): Boolean =
        withContext(Dispatchers.IO) {
            val j =
                JSONObject(
                    call(
                        "/auth/v1/signup?redirect_to=" + Uri.encode(emailRedirect),
                        "POST",
                        JSONObject().put("email", email).put("password", password),
                    )
                )
            save(j)
            j.has("access_token")
        }

    suspend fun recover(email: String) =
        withContext(Dispatchers.IO) {
            call(
                "/auth/v1/recover?redirect_to=" + Uri.encode(emailRedirect),
                "POST",
                JSONObject().put("email", email),
            )
            Unit
        }

    fun googleUrl(): Uri {
        val bytes = ByteArray(32).apply { SecureRandom().nextBytes(this) }
        val verifier =
            Base64.encodeToString(bytes, Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP)
        prefs.edit {
            putString("pkce", verifier)
            putLong("pkceAt", System.currentTimeMillis())
        }
        val challenge =
            Base64.encodeToString(
                MessageDigest.getInstance("SHA-256").digest(verifier.toByteArray()),
                Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP,
            )
        return (cfg.getString("url") + "/auth/v1/authorize")
            .toUri()
            .buildUpon()
            .appendQueryParameter("provider", "google")
            .appendQueryParameter("redirect_to", redirect)
            .appendQueryParameter("code_challenge", challenge)
            .appendQueryParameter("code_challenge_method", "s256")
            .build()
    }

    suspend fun callback(uri: Uri) =
        withContext(Dispatchers.IO) {
            require(uri.scheme == "brainstem" && uri.host == "auth" && uri.path == "/callback")
            uri.getQueryParameter("error_description")?.let { error(it) }
            val code = uri.getQueryParameter("code") ?: error("Google ไม่ได้ส่งรหัสยืนยันกลับมา")
            val verifier = prefs.getString("pkce", null) ?: error("กรุณาเริ่มเข้าสู่ระบบใหม่")
            require(System.currentTimeMillis() - prefs.getLong("pkceAt", 0) < 600000) {
                "ลิงก์หมดอายุ · กรุณาเข้าสู่ระบบใหม่"
            }
            try {
                save(
                    JSONObject(
                        call(
                            "/auth/v1/token?grant_type=pkce",
                            "POST",
                            JSONObject().put("auth_code", code).put("code_verifier", verifier),
                        )
                    )
                )
            } finally {
                prefs.edit {
                    remove("pkce")
                    remove("pkceAt")
                }
            }
        }

    suspend fun profiles(): List<Profile> =
        withContext(Dispatchers.IO) {
            JSONArray(call("/rest/v1/app_profile?select=*&order=created_at", token = token()))
                .objects()
                .map { j ->
                    val birth =
                        runCatching { java.time.LocalDate.parse(j.optString("birth_date")) }
                            .getOrNull()
                    val age =
                        birth?.let { java.time.Period.between(it, java.time.LocalDate.now()).years }
                            ?: 9
                    Profile(
                        j.getString("id"),
                        j.optString("nickname").takeUnless { it.isBlank() || it == "null" }
                            ?: j.getString("real_name"),
                        age,
                        j.getString("id"),
                        j.getString("real_name"),
                        j.optString("sex").nullToEmpty(),
                        j.optString("birth_date").nullToEmpty(),
                        j.optString("type", "kid"),
                        j.optString("handedness").nullToEmpty(),
                        j.optString("guardian_name").nullToEmpty(),
                        j.optString("guardian_relation").nullToEmpty(),
                        j.optString("guardian_phone").nullToEmpty(),
                        photo =
                            j.optString("photo_path")
                                .nullToEmpty()
                                .takeIf { it.isNotBlank() }
                                ?.let { path ->
                                    runCatching {
                                            Base64.encodeToString(
                                                request(
                                                    "/storage/v1/object/authenticated/profile-photos/$path",
                                                    token = token(),
                                                ),
                                                Base64.NO_WRAP,
                                            )
                                        }
                                        .getOrDefault("")
                                }
                                .orEmpty(),
                        photoPath = j.optString("photo_path").nullToEmpty(),
                    )
                }
        }

    suspend fun saveProfile(profile: Profile): Profile =
        withContext(Dispatchers.IO) {
            photoUploadFailed = false
            val row =
                JSONObject()
                    .put("real_name", profile.realName)
                    .put("nickname", profile.name)
                    .put("sex", profile.sex.ifBlank { null } ?: JSONObject.NULL)
                    .put("birth_date", profile.birth)
                    .put("type", profile.type)
                    .put("handedness", profile.hand.ifBlank { null } ?: JSONObject.NULL)
                    .put("local_code", profile.id)
                    .put("guardian_name", profile.guardianName.ifBlank { null } ?: JSONObject.NULL)
                    .put(
                        "guardian_relation",
                        profile.guardianRelation.ifBlank { null } ?: JSONObject.NULL,
                    )
                    .put(
                        "guardian_phone",
                        profile.guardianPhone.ifBlank { null } ?: JSONObject.NULL,
                    )
            var result = profile
            if (profile.cloud.isNotBlank())
                call(
                    "/rest/v1/app_profile?id=eq.${Uri.encode(profile.cloud)}",
                    "PATCH",
                    row,
                    token(),
                )
            else {
                row.put("consent_at", java.time.Instant.now().toString())
                val saved =
                    JSONArray(
                            call(
                                "/rest/v1/app_profile",
                                "POST",
                                row,
                                token(),
                                mapOf("Prefer" to "return=representation"),
                            )
                        )
                        .getJSONObject(0)
                result = profile.copy(cloud = saved.getString("id"))
            }
            // Photo failure must never discard a successfully saved profile.
            runCatching {
                    val path =
                        if (result.photo.isNotBlank()) {
                            val target = "${accountId()}/${result.cloud}.jpg"
                            request(
                                "/storage/v1/object/profile-photos/$target",
                                "POST",
                                Base64.decode(result.photo, Base64.DEFAULT),
                                token(),
                                mapOf("Content-Type" to "image/jpeg", "x-upsert" to "true"),
                            )
                            target
                        } else {
                            if (result.photoPath.isNotBlank())
                                request(
                                    "/storage/v1/object/profile-photos/${result.photoPath}",
                                    "DELETE",
                                    token = token(),
                                )
                            ""
                        }
                    if (path != result.photoPath) {
                        call(
                            "/rest/v1/app_profile?id=eq.${result.cloud}",
                            "PATCH",
                            JSONObject().put("photo_path", path.ifBlank { JSONObject.NULL }),
                            token(),
                        )
                        result = result.copy(photoPath = path)
                    }
                }
                .onFailure { photoUploadFailed = true }
            result
        }

    private fun String.nullToEmpty() = if (this == "null") "" else this

    suspend fun signOut() =
        withContext(Dispatchers.IO) {
            try {
                session()?.let {
                    call("/auth/v1/logout", "POST", token = it.getString("access_token"))
                }
            } finally {
                clear()
            }
        }

    private fun message(s: String) =
        when {
            s.contains("invalid login", true) -> "อีเมลหรือรหัสผ่านไม่ถูกต้อง"
            s.contains("not confirmed", true) -> "ยังไม่ได้ยืนยันอีเมล · เปิดลิงก์ในอีเมลก่อน"
            s.contains("rate limit", true) || s.contains("too many", true) ->
                "ส่งบ่อยเกินไป · รอสักครู่แล้วลองใหม่"
            else -> s
        }
}
