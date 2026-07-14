package com.ramerlabs.scanelite.license

import android.content.Context
import android.provider.Settings
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

data class LicensePublicStatus(
    val valid: Boolean,
    val message: String,
    val buyUrl: String = LicenseConstants.BUY_URL
)

data class LicenseActivateResult(
    val success: Boolean,
    val message: String,
    val buyUrl: String = LicenseConstants.BUY_URL
)

object LicenseConstants {
    const val PRODUCT_SLUG = "scanelite"
    const val BUY_URL = "https://ramerlabs.com/product/scanelite/"
    const val PREFS = "scanelite_license"
    const val KEY_LICENSE = "license_key"
    const val KEY_VALID = "valid"
    const val KEY_VALIDATED_AT = "last_validated_at"
    const val REVALIDATE_MS = 6L * 60L * 60L * 1000L
}

@Singleton
class LicenseRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs = context.getSharedPreferences(LicenseConstants.PREFS, Context.MODE_PRIVATE)

    fun installSiteUrl(): String {
        val androidId = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
            ?: "unknown"
        return "https://scanelite.app/install/$androidId"
    }

    fun publicStatus(): LicensePublicStatus {
        val valid = prefs.getBoolean(LicenseConstants.KEY_VALID, false) &&
            !prefs.getString(LicenseConstants.KEY_LICENSE, null).isNullOrBlank()
        return LicensePublicStatus(
            valid = valid,
            message = if (valid) {
                "License active."
            } else {
                "A valid license key is required to use ScanElite."
            }
        )
    }

    suspend fun activate(rawKey: String): LicenseActivateResult = withContext(Dispatchers.IO) {
        val key = rawKey.trim()
        if (key.isEmpty()) {
            return@withContext LicenseActivateResult(false, "Enter a license key.")
        }
        val body = JSONObject()
            .put("license_key", key)
            .put("product_slug", LicenseConstants.PRODUCT_SLUG)
            .put("site_url", installSiteUrl())
            .put("site_name", "ScanElite")
        val res = postJson("activate", body)
        val success = res.optBoolean("success", false) || res.optBoolean("valid", false)
        if (!success) {
            return@withContext LicenseActivateResult(
                false,
                res.optString(
                    "message",
                    res.optString("error", "Invalid license key. Buy a license at ramerlabs.com.")
                )
            )
        }
        prefs.edit()
            .putString(LicenseConstants.KEY_LICENSE, key)
            .putBoolean(LicenseConstants.KEY_VALID, true)
            .putLong(LicenseConstants.KEY_VALIDATED_AT, System.currentTimeMillis())
            .apply()
        LicenseActivateResult(true, res.optString("message", "License activated."))
    }

    suspend fun validateStored(force: Boolean = false): Boolean = withContext(Dispatchers.IO) {
        val key = prefs.getString(LicenseConstants.KEY_LICENSE, null) ?: return@withContext false
        val last = prefs.getLong(LicenseConstants.KEY_VALIDATED_AT, 0L)
        val age = System.currentTimeMillis() - last
        if (!force && prefs.getBoolean(LicenseConstants.KEY_VALID, false) && age in 0 until LicenseConstants.REVALIDATE_MS) {
            return@withContext true
        }
        val body = JSONObject()
            .put("license_key", key)
            .put("product_slug", LicenseConstants.PRODUCT_SLUG)
            .put("site_url", installSiteUrl())
        val res = postJson("validate", body)
        val valid = res.optBoolean("success", false) || res.optBoolean("valid", false)
        prefs.edit()
            .putBoolean(LicenseConstants.KEY_VALID, valid)
            .putLong(LicenseConstants.KEY_VALIDATED_AT, System.currentTimeMillis())
            .apply()
        valid
    }

    fun clear() {
        prefs.edit().clear().apply()
    }

    private fun postJson(endpoint: String, body: JSONObject): JSONObject {
        val url = URL("${RlmInternal.licenseServerUrl()}/wp-json/ramerlabs-license/v1/$endpoint")
        val conn = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 30000
            readTimeout = 30000
            doOutput = true
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("Accept", "application/json")
        }
        try {
            OutputStreamWriter(conn.outputStream).use { it.write(body.toString()) }
            val stream = if (conn.responseCode in 200..299) conn.inputStream else conn.errorStream
            val text = BufferedReader(InputStreamReader(stream ?: return JSONObject())).use { it.readText() }
            return try {
                JSONObject(text)
            } catch (_: Exception) {
                JSONObject().put("message", "Unexpected license server response.")
            }
        } catch (e: Exception) {
            return JSONObject().put("message", "Could not reach license server. Check your connection.")
        } finally {
            conn.disconnect()
        }
    }
}
