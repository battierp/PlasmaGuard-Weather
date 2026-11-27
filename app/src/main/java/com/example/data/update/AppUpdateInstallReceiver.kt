package com.example.data.update

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import androidx.core.content.IntentCompat

class AppUpdateInstallReceiver : BroadcastReceiver() {
  override fun onReceive(context: Context, intent: Intent) {
    val status = intent.getIntExtra(PackageInstaller.EXTRA_STATUS, PackageInstaller.STATUS_FAILURE)
    val rawMessage = intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE).orEmpty().trim()

    when (status) {
      PackageInstaller.STATUS_PENDING_USER_ACTION -> {
        val confirmIntent =
          IntentCompat.getParcelableExtra(intent, Intent.EXTRA_INTENT, Intent::class.java)
        if (confirmIntent != null) {
          confirmIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
          runCatching { context.startActivity(confirmIntent) }
            .onFailure { err ->
              AppUpdateManager.clearPendingUpdateRelaunch(context)
              AppUpdateManager.reportInstallStatus(
                AppUpdateInstallStatus.Failed(
                  err.localizedMessage ?: "Unable to open Android update confirmation."
                )
              )
            }
        } else {
          AppUpdateManager.clearPendingUpdateRelaunch(context)
          AppUpdateManager.reportInstallStatus(
            AppUpdateInstallStatus.Failed("Missing confirmation prompt from Android installer.")
          )
        }
      }
      PackageInstaller.STATUS_SUCCESS -> {
        AppUpdateManager.reportInstallStatus(AppUpdateInstallStatus.Succeeded)
      }
      else -> {
        AppUpdateManager.clearPendingUpdateRelaunch(context)
        AppUpdateManager.reportInstallStatus(
          AppUpdateInstallStatus.Failed(formatInstallFailureMessage(status, rawMessage))
        )
      }
    }
  }
}
