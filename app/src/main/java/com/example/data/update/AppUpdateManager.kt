package com.example.data.update

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import com.example.BuildConfig
import com.example.data.remote.NetworkModule
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import okhttp3.Request

object AppUpdateManager {
  private const val UPDATES_DIR_NAME = "updates"
  private const val APK_MIME_TYPE = "application/vnd.android.package-archive"

  fun canRequestPackageInstalls(context: Context): Boolean =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      context.packageManager.canRequestPackageInstalls()
    } else {
      true
    }

  fun createUnknownSourcesPermissionIntent(context: Context): Intent =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      Intent(
        Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
        Uri.parse("package:${context.packageName}"),
      ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    } else {
      Intent(Settings.ACTION_SECURITY_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }

  fun launchApkInstall(context: Context, apkFile: File): Result<Unit> =
    runCatching {
      require(apkFile.exists() && apkFile.length() > 0L) {
        "Downloaded update file is missing."
      }
      val authority = "${BuildConfig.APPLICATION_ID}.fileprovider"
      val contentUri = FileProvider.getUriForFile(context, authority, apkFile)
      val intent =
        Intent(Intent.ACTION_VIEW).apply {
          setDataAndType(contentUri, APK_MIME_TYPE)
          addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
          addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
      context.startActivity(intent)
    }

  suspend fun downloadApk(
    context: Context,
    downloadUrl: String,
    versionLabel: String,
    onProgress: (downloadedBytes: Long, totalBytes: Long) -> Unit,
  ): File =
    withContext(Dispatchers.IO) {
      require(downloadUrl.startsWith("https://", ignoreCase = true)) {
        "Update URL must use HTTPS."
      }

      val updatesDir = File(context.cacheDir, UPDATES_DIR_NAME).apply { mkdirs() }
      updatesDir.listFiles()?.forEach { oldFile -> oldFile.delete() }

      val safeVersion = versionLabel.replace(Regex("[^0-9A-Za-z._-]"), "_")
      val tempFile = File(updatesDir, "PlasmaSky-v$safeVersion.apk.part")
      val targetFile = File(updatesDir, "PlasmaSky-v$safeVersion.apk")

      val request =
        Request.Builder()
          .url(downloadUrl)
          .header("User-Agent", "PlasmaSky-AndroidTV")
          .build()

      NetworkModule.downloadHttpClient.newCall(request).execute().use { response ->
        if (!response.isSuccessful) {
          throw IOException("Download failed (HTTP ${response.code}).")
        }
        val body = response.body ?: throw IOException("Empty download response.")
        val totalBytes = body.contentLength()

        body.byteStream().use { input ->
          FileOutputStream(tempFile).use { output ->
            val buffer = ByteArray(8192)
            var downloaded = 0L
            var lastReported = -1L

            while (true) {
              coroutineContext.ensureActive()
              val read = input.read(buffer)
              if (read == -1) break
              output.write(buffer, 0, read)
              downloaded += read
              if (downloaded - lastReported >= 65_536L || (totalBytes > 0L && downloaded >= totalBytes)) {
                lastReported = downloaded
                onProgress(downloaded, totalBytes)
              }
            }
            output.flush()
          }
        }
      }

      if (!hasZipApkHeader(tempFile)) {
        tempFile.delete()
        throw IOException("Downloaded file is not a valid Android APK package.")
      }

      if (!tempFile.renameTo(targetFile)) {
        tempFile.copyTo(targetFile, overwrite = true)
        tempFile.delete()
      }
      targetFile
    }

  internal fun hasZipApkHeader(file: File): Boolean {
    if (!file.exists() || file.length() < 4L) return false
    return file.inputStream().use { stream ->
      val b0 = stream.read()
      val b1 = stream.read()
      b0 == 0x50 && b1 == 0x4B
    }
  }
}
