package com.example.data.update

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageInstaller
import android.content.pm.PackageManager
import android.content.pm.Signature
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import androidx.core.content.pm.PackageInfoCompat
import com.example.BuildConfig
import com.example.data.remote.NetworkModule
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.security.MessageDigest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import okhttp3.Request

sealed interface ApkCompatibilityResult {
  data object Compatible : ApkCompatibilityResult

  data class Incompatible(val reason: String) : ApkCompatibilityResult
}

sealed interface AppUpdateInstallStatus {
  data object Idle : AppUpdateInstallStatus

  data object Installing : AppUpdateInstallStatus

  data object Succeeded : AppUpdateInstallStatus

  data class Failed(val message: String) : AppUpdateInstallStatus
}

internal fun formatInstallFailureMessage(status: Int, statusMessage: String): String {
  val normalized = statusMessage.uppercase()
  return when {
    normalized.contains("UPDATE_INCOMPATIBLE") ||
      normalized.contains("INCONSISTENT_CERTIFICATES") ||
      normalized.contains("SIGNATURES") ->
      "Install blocked: signing certificate does not match the installed version. Uninstall PlasmaSky once, then install the new release cleanly."
    normalized.contains("VERSION_DOWNGRADE") ->
      "Install blocked: the downloaded build has an older version code than the installed version."
    status == PackageInstaller.STATUS_FAILURE_ABORTED ->
      "Installation was canceled."
    status == PackageInstaller.STATUS_FAILURE_CONFLICT ->
      "Install conflict with the currently installed app. Uninstall the older build once and install cleanly."
    status == PackageInstaller.STATUS_FAILURE_INCOMPATIBLE ->
      "This APK is incompatible with your device or installed signature."
    status == PackageInstaller.STATUS_FAILURE_STORAGE ->
      "Not enough free storage space on the device to install the update."
    status == PackageInstaller.STATUS_FAILURE_INVALID ->
      "The downloaded APK package appears invalid or corrupted."
    statusMessage.isNotBlank() -> statusMessage
    else -> "Android package installer failed (code $status)."
  }
}

object AppUpdateManager {
  private const val UPDATES_DIR_NAME = "updates"
  private const val APK_MIME_TYPE = "application/vnd.android.package-archive"
  private const val INSTALL_CALLBACK_REQUEST_CODE = 4207

  private val _installStatus = MutableStateFlow<AppUpdateInstallStatus>(AppUpdateInstallStatus.Idle)
  val installStatus: StateFlow<AppUpdateInstallStatus> = _installStatus.asStateFlow()

  internal fun reportInstallStatus(status: AppUpdateInstallStatus) {
    _installStatus.value = status
  }

  fun clearInstallStatus() {
    _installStatus.value = AppUpdateInstallStatus.Idle
  }

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

  fun openUnknownSourcesPermissionSettings(context: Context): Result<Unit> =
    runCatching {
      context.startActivity(createUnknownSourcesPermissionIntent(context))
    }.recoverCatching {
      val appDetailsIntent =
        Intent(
          Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
          Uri.parse("package:${context.packageName}"),
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
      context.startActivity(appDetailsIntent)
    }

  fun verifyApkCompatibility(context: Context, apkFile: File): ApkCompatibilityResult {
    if (!hasZipApkHeader(apkFile)) {
      return ApkCompatibilityResult.Incompatible(
        "Downloaded file is not a valid Android APK package."
      )
    }

    val pm = context.packageManager
    val archiveInfo =
      getArchiveInfoWithSigners(pm, apkFile.absolutePath)
        ?: return ApkCompatibilityResult.Incompatible(
          "Android could not parse the downloaded APK package."
        )
    val installedInfo = getInstalledInfoWithSigners(pm, context.packageName)

    return checkPackageCompatibility(
      installedPackageName = context.packageName,
      installedVersionCode = installedInfo?.let { PackageInfoCompat.getLongVersionCode(it) },
      installedSignerDigests = installedInfo?.let(::extractSignerSha256Digests).orEmpty(),
      archivePackageName = archiveInfo.packageName,
      archiveVersionCode = PackageInfoCompat.getLongVersionCode(archiveInfo),
      archiveSignerDigests = extractSignerSha256Digests(archiveInfo),
    )
  }

  internal fun checkPackageCompatibility(
    installedPackageName: String,
    installedVersionCode: Long?,
    installedSignerDigests: Set<String>,
    archivePackageName: String?,
    archiveVersionCode: Long?,
    archiveSignerDigests: Set<String>,
  ): ApkCompatibilityResult {
    if (!archivePackageName.isNullOrBlank() && archivePackageName != installedPackageName) {
      return ApkCompatibilityResult.Incompatible(
        "Update package ID ($archivePackageName) does not match installed app ($installedPackageName)."
      )
    }
    if (
      installedVersionCode != null &&
        archiveVersionCode != null &&
        archiveVersionCode < installedVersionCode
    ) {
      return ApkCompatibilityResult.Incompatible(
        "Downloaded update build ($archiveVersionCode) is older than the installed build ($installedVersionCode)."
      )
    }
    if (
      installedSignerDigests.isNotEmpty() &&
        archiveSignerDigests.isNotEmpty() &&
        installedSignerDigests.intersect(archiveSignerDigests).isEmpty()
    ) {
      val installedShort = installedSignerDigests.first().take(8)
      val archiveShort = archiveSignerDigests.first().take(8)
      return ApkCompatibilityResult.Incompatible(
        "Cannot install directly over the current version because their signing certificates don't match (installed=$installedShort…, update=$archiveShort…). Uninstall PlasmaSky once, then install this release from GitHub Releases so future in-app updates work seamlessly."
      )
    }
    return ApkCompatibilityResult.Compatible
  }

  suspend fun installApkInApp(context: Context, apkFile: File): Result<Unit> =
    withContext(Dispatchers.IO) {
      runCatching {
        require(apkFile.exists() && apkFile.length() > 0L) {
          "Downloaded update file is missing."
        }
        _installStatus.value = AppUpdateInstallStatus.Installing

        val sessionInstallResult = runCatching { installViaPackageInstallerSession(context, apkFile) }
        if (sessionInstallResult.isFailure) {
          // Fallback to direct FileProvider package-archive intent (never a browser URL)
          withContext(Dispatchers.Main) {
            launchApkInstallViaIntent(context, apkFile).getOrThrow()
          }
          _installStatus.value = AppUpdateInstallStatus.Idle
        }
      }.onFailure { err ->
        _installStatus.value =
          AppUpdateInstallStatus.Failed(
            err.localizedMessage ?: "Unable to start Android package installer."
          )
      }
    }

  private fun installViaPackageInstallerSession(context: Context, apkFile: File) {
    val packageInstaller = context.packageManager.packageInstaller
    val params =
      PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL).apply {
        setAppPackageName(context.packageName)
        setSize(apkFile.length())
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
          setRequireUserAction(PackageInstaller.SessionParams.USER_ACTION_NOT_REQUIRED)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
          setPackageSource(PackageInstaller.PACKAGE_SOURCE_DOWNLOADED_FILE)
        }
      }

    var sessionId = -1
    var committed = false
    try {
      sessionId = packageInstaller.createSession(params)
      packageInstaller.openSession(sessionId).use { session ->
        session.openWrite("PlasmaSky-update.apk", 0L, apkFile.length()).use { out ->
          FileInputStream(apkFile).use { input ->
            input.copyTo(out)
          }
          session.fsync(out)
        }

        val callbackIntent =
          Intent(context, AppUpdateInstallReceiver::class.java).apply {
            setPackage(context.packageName)
          }
        val pendingFlags =
          if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
          } else {
            PendingIntent.FLAG_UPDATE_CURRENT
          }
        val pendingIntent =
          PendingIntent.getBroadcast(
            context,
            INSTALL_CALLBACK_REQUEST_CODE,
            callbackIntent,
            pendingFlags,
          )
        session.commit(pendingIntent.intentSender)
        committed = true
      }
    } finally {
      if (!committed && sessionId != -1) {
        runCatching { packageInstaller.abandonSession(sessionId) }
      }
    }
  }

  fun launchApkInstallViaIntent(context: Context, apkFile: File): Result<Unit> =
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

  private fun getInstalledInfoWithSigners(pm: PackageManager, packageName: String): PackageInfo? {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
      val modern =
        runCatching {
          pm.getPackageInfo(packageName, PackageManager.GET_SIGNING_CERTIFICATES)
        }.getOrNull()
      if (modern != null && extractSignerSha256Digests(modern).isNotEmpty()) {
        return modern
      }
    }
    return runCatching {
      @Suppress("DEPRECATION")
      pm.getPackageInfo(packageName, PackageManager.GET_SIGNATURES)
    }.getOrNull()
  }

  private fun getArchiveInfoWithSigners(pm: PackageManager, apkPath: String): PackageInfo? {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
      val modern =
        runCatching {
          pm.getPackageArchiveInfo(apkPath, PackageManager.GET_SIGNING_CERTIFICATES)
        }.getOrNull()
      if (modern != null && extractSignerSha256Digests(modern).isNotEmpty()) {
        return modern
      }
    }
    return runCatching {
      @Suppress("DEPRECATION")
      pm.getPackageArchiveInfo(apkPath, PackageManager.GET_SIGNATURES)
    }.getOrNull()
      ?: runCatching { pm.getPackageArchiveInfo(apkPath, 0) }.getOrNull()
  }

  private fun extractSignerSha256Digests(info: PackageInfo): Set<String> {
    val sigs = ArrayList<Signature>()
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
      info.signingInfo?.let { signingInfo ->
        signingInfo.apkContentsSigners?.let { sigs.addAll(it) }
        signingInfo.signingCertificateHistory?.let { sigs.addAll(it) }
      }
    }
    if (sigs.isEmpty()) {
      @Suppress("DEPRECATION")
      info.signatures?.let { sigs.addAll(it) }
    }
    if (sigs.isEmpty()) return emptySet()

    val digest = MessageDigest.getInstance("SHA-256")
    return sigs
      .map { signature ->
        digest.digest(signature.toByteArray()).joinToString("") { byte -> "%02x".format(byte) }
      }
      .toSet()
  }
}
