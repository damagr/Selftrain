package com.selftrain.app.util

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import java.io.File
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL

data class GitHubAsset(
    @SerializedName("browser_download_url") val browserDownloadUrl: String
)

data class GitHubRelease(
    @SerializedName("tag_name") val tagName: String,
    @SerializedName("html_url") val htmlUrl: String,
    @SerializedName("assets") val assets: List<GitHubAsset> = emptyList()
)

class UpdateChecker(private val cacheDir: File) {
    private val gson = Gson()
    private val apiUrl = "https://api.github.com/repos/damagr/Selftrain/releases/latest"

    fun checkForUpdate(currentVersion: String): GitHubRelease? {
        val conn = URL(apiUrl).openConnection() as HttpURLConnection
        conn.setRequestProperty("Accept", "application/vnd.github+json")
        conn.useCaches = false
        conn.connectTimeout = 10_000
        conn.readTimeout = 10_000

        return try {
            if (conn.responseCode != 200) return null
            val json = conn.inputStream.bufferedReader().readText()
            val release = gson.fromJson(json, GitHubRelease::class.java)
            if (isNewer(release.tagName.removePrefix("v"), currentVersion)) release else null
        } catch (_: Exception) {
            null
        } finally {
            conn.disconnect()
        }
    }

    fun downloadApk(url: String, onProgress: (Int) -> Unit): File {
        val conn = URL(url).openConnection() as HttpURLConnection
        conn.connectTimeout = 10_000
        conn.readTimeout = 120_000

        try {
            conn.inputStream.use { input ->
                val total = conn.contentLength
                val file = File(cacheDir, "update.apk")
                file.outputStream().use { output ->
                    val buffer = ByteArray(8192)
                    var downloaded = 0L
                    var bytes: Int
                    while (input.read(buffer).also { bytes = it } != -1) {
                        output.write(buffer, 0, bytes)
                        downloaded += bytes
                        if (total > 0) {
                            onProgress(((downloaded * 100) / total).toInt())
                        }
                    }
                }
                return file
            }
        } finally {
            conn.disconnect()
        }
    }

    private fun isNewer(remote: String, local: String): Boolean {
        val remoteParts = remote.split(".").map { it.toIntOrNull() ?: 0 }
        val localParts = local.split(".").map { it.toIntOrNull() ?: 0 }
        val maxLen = maxOf(remoteParts.size, localParts.size)

        for (i in 0 until maxLen) {
            val r = remoteParts.getOrElse(i) { 0 }
            val l = localParts.getOrElse(i) { 0 }
            if (r > l) return true
            if (r < l) return false
        }
        return false
    }
}
