package com.ashna.streakkeeper

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import androidx.core.net.toUri

object TikTokHelper {

    private const val TIKTOK_PACKAGE = "com.zhiliaoapp.musically"

    /**
     * Fastest supported route to TikTok: launches the installed TikTok app
     * (package launch intent, falling back to the tiktok:// URI scheme).
     * Returns false when TikTok is not installed — no silent failure.
     * Deep-linking into a specific private conversation is not supported by
     * TikTok for Personal accounts, so the app opens TikTok and the user
     * picks the chat (documented limitation, not fake automation).
     */
    fun openTikTok(context: Context): Boolean {
        val intent = context.packageManager.getLaunchIntentForPackage(TIKTOK_PACKAGE)
            ?: Intent(Intent.ACTION_VIEW, "tiktok://".toUri())
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return try {
            context.startActivity(intent)
            true
        } catch (e: ActivityNotFoundException) {
            false
        }
    }
}
