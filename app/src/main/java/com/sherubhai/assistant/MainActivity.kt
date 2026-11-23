package com.sherubhai.assistant

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.camera2.CameraManager
import android.os.Bundle
import android.view.ViewGroup
import android.widget.*
import java.util.Locale

class MainActivity : Activity() {
    private lateinit var result: TextView
    private lateinit var command: EditText
    private var torchId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
        layout.addView(title)
        layout.addView(result)
        layout.addView(command, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        layout.addView(send)
        setContentView(layout)
    }

    private fun execute(raw: String) {
        val text = raw.lowercase(Locale.ROOT).trim()
        result.text = when {
            text == "torch on" || text == "flashlight on" || text == "turn on torch" -> setTorch(true)
            text == "torch off" || text == "flashlight off" || text == "turn off torch" -> setTorch(false)
            text.startsWith("open ") -> openApp(text.removePrefix("open ").trim())
            else -> "Not supported yet. Try torch on/off or open calculator."
        }
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
