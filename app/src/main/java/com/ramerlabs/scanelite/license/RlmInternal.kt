package com.ramerlabs.scanelite.license

/**
 * Private license server URL assembly.
 * Never show this URL in customer-facing UI, errors, or settings.
 */
internal object RlmInternal {
    fun licenseServerUrl(): String {
        val parts = arrayOf("https://", "ramerlabs", ".com")
        return parts.joinToString("").trimEnd('/')
    }
}
