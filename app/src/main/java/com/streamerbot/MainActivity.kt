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
                if (!isAccessibilityServiceEnabled()) {
                    requestAccessibilityPermission()
                } else {
                    startAccessibilityService();
                }
            }
        }

        setContentView(startButton)
    }

    private fun startAccessibilityService() {
        if (!isServiceRunning(MainService::class.java)) {
            val intent = Intent(this, MainService::class.java)
            startService(intent)
            Toast.makeText(this, "Accessibility Service started.", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Service is already running.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun isServiceRunning(serviceClass: Class<*>): Boolean {
        val manager = getSystemService(Context.ACTIVITY_SERVICE) as android.app.ActivityManager
        for (service in manager.getRunningServices(Int.MAX_VALUE)) {
            if (serviceClass.name == service.service.className) {
                return true
            }
        }
        return false
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
