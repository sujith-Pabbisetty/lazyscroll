package com.example.lazyscroll

import android.Manifest
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    private lateinit var btnOverlay: Button
    private lateinit var btnAccessibility: Button
    private lateinit var btnCamera: Button
    private lateinit var btnToggleControls: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Shortcut logic: If everything is already granted, toggle and minimize
        if (isAllPermissionGranted()) {
            toggleControlBar()
            // We use moveTaskToBack instead of finish() so the app stays in Recent Apps.
            // This allows the user to "kill" the service by swiping it away for safety.
            moveTaskToBack(true)
            return
        }

        val rootLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setBackgroundColor(Color.parseColor("#121212"))
            setPadding(50, 50, 50, 50)
        }

        val title = TextView(this).apply {
            text = "LazyScroll Setup"
            textSize = 28f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 60)
        }
        rootLayout.addView(title)

        // 1. Overlay Permission
        btnOverlay = Button(this).apply {
            text = "1. Grant Overlay Permission"
            setTextColor(Color.WHITE)
            textSize = 14f
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(0, 0, 0, 30) }

            setOnClickListener {
                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:$packageName")
                )
                startActivity(intent)
            }
        }
        rootLayout.addView(btnOverlay)

        // 2. Camera Permission
        btnCamera = Button(this).apply {
            text = "2. Grant Camera Permission"
            setTextColor(Color.WHITE)
            textSize = 14f
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(0, 0, 0, 30) }

            setOnClickListener {
                if (ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.CAMERA) 
                    != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(this@MainActivity, arrayOf(Manifest.permission.CAMERA), 101)
                } else {
                    Toast.makeText(this@MainActivity, "Camera Permission already granted", Toast.LENGTH_SHORT).show()
                }
            }
        }
        rootLayout.addView(btnCamera)

        // 3. Accessibility Permission
        btnAccessibility = Button(this).apply {
            text = "3. Enable Accessibility Service"
            setTextColor(Color.WHITE)
            textSize = 14f
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(0, 0, 0, 60) }

            setOnClickListener {
                val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                startActivity(intent)
            }
        }
        rootLayout.addView(btnAccessibility)

        // 4. Toggle Controls
        btnToggleControls = Button(this).apply {
            text = "Show/Hide Control Bar"
            setTextColor(Color.BLACK)
            textSize = 18f
            background = GradientDrawable().apply {
                setColor(Color.YELLOW)
                cornerRadius = 20f
            }
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                150
            )

            setOnClickListener {
                if (isAccessibilityServiceEnabled()) {
                    toggleControlBar()
                    moveTaskToBack(true)
                } else {
                    Toast.makeText(this@MainActivity, "Please enable Accessibility first!", Toast.LENGTH_SHORT).show()
                }
            }
        }
        rootLayout.addView(btnToggleControls)

        setContentView(rootLayout)
    }

    private fun toggleControlBar() {
        val intent = Intent(this, MyAccessibilityService::class.java).apply {
            action = MyAccessibilityService.ACTION_TOGGLE_CONTROLS
        }
        startService(intent)
    }

    private fun isAllPermissionGranted(): Boolean {
        return Settings.canDrawOverlays(this) &&
               ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED &&
               isAccessibilityServiceEnabled()
    }

    override fun onResume() {
        super.onResume()
        updateButtonStates()
    }

    private fun updateButtonStates() {
        if (!::btnOverlay.isInitialized) return
        
        // Overlay State
        if (Settings.canDrawOverlays(this)) {
            btnOverlay.text = "Overlay: GRANTED"
            btnOverlay.background = GradientDrawable().apply {
                setColor(Color.parseColor("#2E7D32"))
                cornerRadius = 15f
            }
        } else {
            btnOverlay.text = "1. Grant Overlay Permission"
            btnOverlay.background = GradientDrawable().apply {
                setColor(Color.parseColor("#D84315"))
                cornerRadius = 15f
            }
        }

        // Camera State
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            btnCamera.text = "Camera: GRANTED"
            btnCamera.background = GradientDrawable().apply {
                setColor(Color.parseColor("#2E7D32"))
                cornerRadius = 15f
            }
        } else {
            btnCamera.text = "2. Grant Camera Permission"
            btnCamera.background = GradientDrawable().apply {
                setColor(Color.parseColor("#D84315"))
                cornerRadius = 15f
            }
        }

        // Accessibility State
        if (isAccessibilityServiceEnabled()) {
            btnAccessibility.text = "Accessibility: ENABLED"
            btnAccessibility.background = GradientDrawable().apply {
                setColor(Color.parseColor("#2E7D32"))
                cornerRadius = 15f
            }
            btnToggleControls.isEnabled = true
            btnToggleControls.alpha = 1.0f
        } else {
            btnAccessibility.text = "3. Enable Accessibility Service"
            btnAccessibility.background = GradientDrawable().apply {
                setColor(Color.parseColor("#D84315"))
                cornerRadius = 15f
            }
            btnToggleControls.isEnabled = false
            btnToggleControls.alpha = 0.5f
        }
    }

    private fun isAccessibilityServiceEnabled(): Boolean {
        val expectedComponentName = ComponentName(this, MyAccessibilityService::class.java)
        val enabledServicesSetting = Settings.Secure.getString(
            contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false
        return enabledServicesSetting.contains(expectedComponentName.flattenToString())
    }
}
