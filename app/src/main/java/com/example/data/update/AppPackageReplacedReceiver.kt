package com.example.data.update

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.MainActivity

class AppPackageReplacedReceiver : BroadcastReceiver() {
  override fun onReceive(context: Context, intent: Intent?) {
    if (intent?.action != Intent.ACTION_MY_PACKAGE_REPLACED) return
    val pending = AppUpdateManager.consumePendingUpdateRelaunchIfValid(context) ?: return

    val launchIntent =
      Intent(context, MainActivity::class.java).apply {
        addFlags(
          Intent.FLAG_ACTIVITY_NEW_TASK or
            Intent.FLAG_ACTIVITY_CLEAR_TOP or
            Intent.FLAG_ACTIVITY_SINGLE_TOP
        )
        putExtra(AppUpdateManager.EXTRA_RELAUNCHED_FROM_UPDATE_VERSION, pending.versionLabel)
      }
    runCatching { context.startActivity(launchIntent) }
  }
}
