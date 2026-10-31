package com.mkr.schedule.util.security

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import java.security.MessageDigest

object AppIntegrity {

    // Debug and release certificates currently used to sign trusted builds.
    private val trustedCertSha256 = setOf(
        "05:3A:5D:2A:26:C7:6E:DC:AE:16:2D:90:38:5D:9C:12:90:B3:D7:F3:1F:20:5D:C8:59:48:69:1F:03:F9:89:42",
        "0F:A2:05:35:3F:AE:C6:7B:B5:C6:91:B3:FC:F2:EF:B1:12:74:87:AC:E7:33:B4:FE:E3:3A:84:B6:49:24:D5:CC",
        "48:94:A0:24:92:75:9E:37:26:19:72:4D:65:D4:51:B6:E9:A7:E9:27:DA:96:E4:0E:60:EB:D7:43:90:A3:46:9C"
    )

    fun isSignatureTrusted(context: Context): Boolean {
        // Local debug certificates differ between development machines.
        if (com.mkr.schedule.BuildConfig.DEBUG) return true
        val signatures = getSignatures(context)
        if (signatures.isEmpty()) return false

        return signatures
            .map { toSha256Hex(it) }
            .any { trustedCertSha256.contains(it) }
    }

    @Suppress("DEPRECATION")
    private fun getSignatures(context: Context): List<ByteArray> {
        val pm = context.packageManager
        val packageName = context.packageName

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val pkgInfo = pm.getPackageInfo(packageName, PackageManager.GET_SIGNING_CERTIFICATES)
            val signingInfo = pkgInfo.signingInfo ?: return emptyList()
            val apkSigners = if (signingInfo.hasMultipleSigners()) {
                signingInfo.apkContentsSigners
            } else {
                signingInfo.signingCertificateHistory
            }
            apkSigners.map { it.toByteArray() }
        } else {
            val pkgInfo = pm.getPackageInfo(packageName, PackageManager.GET_SIGNATURES)
            pkgInfo.signatures?.map { it.toByteArray() } ?: emptyList()
        }
    }

    private fun toSha256Hex(raw: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(raw)
        return digest.joinToString(":") { "%02X".format(it) }
    }
}
