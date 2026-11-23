package com.sherubhai.assistant

import android.app.Notification
import android.app.PendingIntent
import android.app.RemoteInput
import android.content.Intent
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification

class SheruNotificationService : NotificationListenerService() {
    companion object {
        var instance: SheruNotificationService? = null
        fun recent(): List<StatusBarNotification> =
            try { instance?.activeNotifications?.sortedByDescending { it.postTime }?.take(20) ?: emptyList() }
            catch (_: Exception) { emptyList() }
    }
    override fun onListenerConnected() { instance = this }
    override fun onListenerDisconnected() { if (instance === this) instance = null }
    override fun onDestroy() { if (instance === this) instance = null; super.onDestroy() }

    fun sendReply(item: StatusBarNotification, message: String): Boolean {
        val action = item.notification.actions?.firstOrNull { a ->
            a.remoteInputs?.any { it.allowFreeFormInput } == true
        } ?: return false
        val inputs = action.remoteInputs ?: return false
        val replyIntent = Intent()
        val results = android.os.Bundle()
        inputs.filter { it.allowFreeFormInput }.forEach { results.putCharSequence(it.resultKey, message) }
        RemoteInput.addResultsToIntent(inputs, replyIntent, results)
        return try {
            action.actionIntent.send(this, 0, replyIntent)
            true
        } catch (_: PendingIntent.CanceledException) { false }
        catch (_: Exception) { false }
    }
}
