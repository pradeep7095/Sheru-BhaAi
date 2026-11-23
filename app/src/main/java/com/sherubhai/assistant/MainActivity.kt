package com.sherubhai.assistant

import android.Manifest
import android.app.AlertDialog
import android.app.Notification
import android.service.notification.StatusBarNotification
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.camera2.CameraManager
import android.os.Bundle
import android.media.AudioManager
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import android.provider.Settings
import android.view.ViewGroup
import android.widget.*
import java.util.Locale

class MainActivity : Activity(), TextToSpeech.OnInitListener {
    private var tts: TextToSpeech? = null
    private var speechReady = false
    private val speechRequest = 201
    private lateinit var result: TextView
    private lateinit var command: EditText
    private var torchId: String? = null
    private var selectedNotification: StatusBarNotification? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        tts = TextToSpeech(this, this)
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(36, 56, 36, 24)
        }
        val title = TextView(this).apply { text = "Sheru Bhai"; textSize = 28f }
        result = TextView(this).apply {
            text = "Safe prototype. Try: torch on, torch off, open calculator."
            textSize = 17f
            setPadding(0, 28, 0, 28)
        }
        command = EditText(this).apply { hint = "Tell Sheru Bhai what to do"; isSingleLine = true }
        val send = Button(this).apply { text = "Run command"; setOnClickListener { execute(command.text.toString()) } }
        val mic = Button(this).apply { text = "Speak command"; setOnClickListener { listen() } }
        layout.addView(title)
        layout.addView(result)
        layout.addView(command, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        layout.addView(send)
        layout.addView(mic)
        val enableNotifications = Button(this).apply {
            text = "Enable notification access"
            setOnClickListener { startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)) }
        }
        val readNotifications = Button(this).apply {
            text = "Show recent notifications"
            setOnClickListener { showNotifications() }
        }
        val draftReply = Button(this).apply {
            text = "Reply to selected notification"
            setOnClickListener { prepareReply() }
        }
        layout.addView(enableNotifications)
        layout.addView(readNotifications)
        layout.addView(draftReply)
        setContentView(layout)
    }

    private fun execute(raw: String) {
        val text = raw.lowercase(Locale.ROOT).trim()
        val answer = when {
            text == "torch on" || text == "flashlight on" || text == "turn on torch" -> setTorch(true)
            text == "torch off" || text == "flashlight off" || text == "turn off torch" -> setTorch(false)
            text == "read notifications" || text == "show notifications" -> { showNotifications(); return }
            text == "reply to notification" -> { prepareReply(); return }
            text == "volume up" || text == "increase volume" -> changeVolume(true)
            text == "volume down" || text == "decrease volume" -> changeVolume(false)
            text == "open settings" -> try { startActivity(Intent(Settings.ACTION_SETTINGS)); "Opening Settings" } catch (e: Exception) { "Settings unavailable" }
            text.startsWith("open ") -> openApp(text.removePrefix("open ").trim())
            else -> "Not supported yet. Try torch on/off, open an app, volume up/down, or open settings."
        }
        result.text = answer
        if (speechReady) tts?.speak(answer, TextToSpeech.QUEUE_FLUSH, null, "sheru-response")
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            speechReady = tts?.setLanguage(Locale.ENGLISH) != TextToSpeech.LANG_MISSING_DATA
        }
    }

    private fun listen() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault().toLanguageTag())
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak to Sheru Bhai")
        }
        try { startActivityForResult(intent, speechRequest) }
        catch (e: Exception) { result.text = "Speech recognition is unavailable on this phone." }
    }

    @Deprecated("Used for compatibility with Android API 26")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == speechRequest && resultCode == RESULT_OK) {
            val heard = data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
            if (!heard.isNullOrBlank()) { command.setText(heard); execute(heard) }
        }
    }

    private fun changeVolume(up: Boolean): String {
        val manager = getSystemService(AUDIO_SERVICE) as AudioManager
        manager.adjustStreamVolume(AudioManager.STREAM_MUSIC,
            if (up) AudioManager.ADJUST_RAISE else AudioManager.ADJUST_LOWER,
            AudioManager.FLAG_SHOW_UI)
        return if (up) "Volume increased" else "Volume decreased"
    }

    override fun onDestroy() {
        tts?.stop()
        tts?.shutdown()
        super.onDestroy()
    }

    private fun showNotifications() {
        if (SheruNotificationService.instance == null) {
            result.text = "Enable notification access in Android Settings first."
            return
        }
        val items = SheruNotificationService.recent().filter {
            val e = it.notification.extras
            !e.getCharSequence(Notification.EXTRA_TITLE).isNullOrBlank() ||
                !e.getCharSequence(Notification.EXTRA_TEXT).isNullOrBlank()
        }
        if (items.isEmpty()) { result.text = "No readable active notifications."; return }
        val labels = items.map {
            val e = it.notification.extras
            val title = e.getCharSequence(Notification.EXTRA_TITLE)?.toString() ?: it.packageName
            val body = e.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: "(no preview)"
            "$title: $body"
        }.toTypedArray()
        AlertDialog.Builder(this)
            .setTitle("Choose notification (local only)")
            .setItems(labels) { _, index ->
                selectedNotification = items[index]
                result.text = labels[index]
            }
            .setNegativeButton("Cancel", null).show()
    }

    private fun prepareReply() {
        val item = selectedNotification
        if (item == null) { result.text = "Select a notification first."; return }
        val input = EditText(this).apply { hint = "Type your reply" }
        AlertDialog.Builder(this).setTitle("Draft reply")
            .setView(input)
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Review") { _, _ ->
                val message = input.text.toString().trim()
                if (message.isBlank()) { result.text = "Empty reply not sent."; return@setPositiveButton }
                AlertDialog.Builder(this)
                    .setTitle("Confirm sending reply")
                    .setMessage("Send this reply through the selected notification?\n\n$message")
                    .setNegativeButton("Cancel", null)
                    .setPositiveButton("Send") { _, _ ->
                        val ok = SheruNotificationService.instance?.sendReply(item, message) == true
                        result.text = if (ok) "Reply submitted to the messaging app." else "This notification does not support direct replies."
                    }.show()
            }.show()
    }

    private fun setTorch(on: Boolean): String {
        if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.CAMERA), 42)
            return "Grant camera permission, then repeat the command."
        }
        return try {
            val manager = getSystemService(CAMERA_SERVICE) as CameraManager
            val id = torchId ?: manager.cameraIdList.firstOrNull {
                manager.getCameraCharacteristics(it).get(android.hardware.camera2.CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
            } ?: return "No flashlight available"
            torchId = id
            manager.setTorchMode(id, on)
            if (on) "Torch on" else "Torch off"
        } catch (e: Exception) { "Torch unavailable: ${e.javaClass.simpleName}" }
    }

    private fun openApp(name: String): String {
        val apps = packageManager.queryIntentActivities(
            Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER), 0
        )
        val match = apps.firstOrNull { it.loadLabel(packageManager).toString().lowercase(Locale.ROOT) == name }
            ?: apps.firstOrNull { it.loadLabel(packageManager).toString().lowercase(Locale.ROOT).contains(name) }
            ?: return "App not found: $name"
        val launch = packageManager.getLaunchIntentForPackage(match.activityInfo.packageName)
            ?: return "App cannot be launched"
        return try { startActivity(launch); "Opening ${match.loadLabel(packageManager)}" }
        catch (e: Exception) { "Unable to open app" }
    }
}
