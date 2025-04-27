package com.streamerbot

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var startButton: Button
    private val handler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        startButton = Button(this).apply {
            text = "Start"
            setOnClickListener {
                if (!hasOverlayPermission()) {
                    requestOverlayPermission()
                } else if (!isAccessibilityServiceEnabled()) {
                    requestAccessibilityPermission()
                } else {
                    Toast.makeText(this@MainActivity, "Starting Service in 2 seconds...", Toast.LENGTH_SHORT).show()
                    handler.postDelayed({
                        startAccessibilityService()
                    }, 2000)
                }
            }
        }

        setContentView(startButton)
    }

    private fun startAccessibilityService() {
        val intent = Intent(this, MainService::class.java)
        startService(intent)
    }

    private fun hasOverlayPermission(): Boolean {
        return Settings.canDrawOverlays(this)
    }

    private fun requestOverlayPermission() {
        Toast.makeText(this, "Need permission to draw over apps", Toast.LENGTH_LONG).show()
        val intent = Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:$packageName")
        )
        startActivity(intent)
    }

    private fun isAccessibilityServiceEnabled(): Boolean {
        val am = getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
        val enabledServices =
            Settings.Secure.getString(contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)
        return enabledServices?.contains(packageName) == true
    }

    private fun requestAccessibilityPermission() {
        Toast.makeText(this, "Need Accessibility Service permission", Toast.LENGTH_LONG).show()
        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
        startActivity(intent)
    }
}
