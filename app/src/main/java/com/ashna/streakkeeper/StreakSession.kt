package com.ashna.streakkeeper

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import com.ashna.streakkeeper.data.ContactEntity

/**
 * Drives the "Streak Session" flow:
 * 1. The user taps "Start session" in the app.
 * 2. The app queues every contact who still needs a send today.
 * 3. For each contact, the app prepares the message (sticker + text),
 *    copies it to the clipboard, and opens TikTok.
 * 4. The user pastes it into the right chat, hits send in TikTok, returns
 *    to the app, and taps "Sent ✓" — then the next contact is prepared.
 *
 * This keeps the user's TikTok time to seconds per person with zero browsing,
 * while every send remains an explicit human action (no fake automation).
 */
object StreakSession {

    /** Builds the exact message the user will paste, e.g. "🦄🔥" or "🦄 day 12! 🔥". */
    fun buildMessage(c: ContactEntity): String {
        val emoji = if (c.stickerEmoji.isBlank()) "🔥" else c.stickerEmoji
        val template = c.messageTemplate.trim()
        return when {
            template.isEmpty() -> emoji
            template.contains("{sticker}") -> template.replace("{sticker}", emoji)
            else -> "$emoji $template"
        }
    }

    /** Copies the prepared message for [c] to the system clipboard. Returns false if unavailable. */
    fun copyMessage(context: Context, c: ContactEntity): Boolean {
        val text = buildMessage(c)
        return try {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            clipboard.setPrimaryClip(ClipData.newPlainText("streak message", text))
            true
        } catch (e: Exception) {
            false
        }
    }
}
