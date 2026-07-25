package com.example.lazyscroll

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.content.Intent
import android.graphics.Color
import android.graphics.Path
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.provider.Settings
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.TextView
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.math.abs

class MyAccessibilityService : AccessibilityService(), LifecycleOwner {

    companion object {
        const val ACTION_TOGGLE_CONTROLS = "com.example.lazyscroll.TOGGLE_CONTROLS"
    }

    private lateinit var windowManager: WindowManager
    private var rootOverlay: LinearLayout? = null
    private lateinit var statusText: TextView
    private lateinit var toggleButton: TextView
    private lateinit var params: WindowManager.LayoutParams
    private lateinit var cameraExecutor: ExecutorService
    
    private val lifecycleRegistry = LifecycleRegistry(this)
    override val lifecycle: Lifecycle get() = lifecycleRegistry

    private var isBlinking = false
    private var lastBlinkTime: Long = 0
    private val blinkThreshold = 0.35f
    private val doubleBlinkInterval = 1500L // 1.5 seconds for easier double-blinking
    
    private var isDetectionEnabled = false

    override fun onCreate() {
        super.onCreate()
        lifecycleRegistry.currentState = Lifecycle.State.CREATED
        cameraExecutor = Executors.newSingleThreadExecutor()
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        lifecycleRegistry.currentState = Lifecycle.State.STARTED
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_TOGGLE_CONTROLS) {
            if (rootOverlay == null) {
                showShortcutOverlay()
            } else {
                removeOverlay()
            }
        }
        // Use START_NOT_STICKY so the service doesn't restart automatically if the app process is killed
        return START_NOT_STICKY
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        // CRITICAL: Stop everything when the app is swiped away from Recents for safety
        shutdownEverything()
    }

    private fun shutdownEverything() {
        removeOverlay()
        stopBlinkDetection()
        // This physically turns the service switch OFF in Android settings for maximum safety
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            disableSelf()
        }
        stopSelf()
    }

    private fun showShortcutOverlay() {
        if (!Settings.canDrawOverlays(this)) return

        rootOverlay = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(25, 15, 25, 15)
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#EE121212")) // Solid dark background
                cornerRadius = 80f
                setStroke(3, Color.YELLOW)
            }
        }

        statusText = TextView(this).apply {
            text = "LazyScroll: Off"
            setTextColor(Color.WHITE)
            textSize = 14f
            setPadding(20, 0, 20, 0)
        }

        toggleButton = TextView(this).apply {
            text = "▶ Start" 
            setTextColor(Color.GREEN)
            textSize = 16f
            setPadding(20, 10, 20, 10)
            setOnClickListener {
                toggleDetection()
            }
        }

        rootOverlay?.addView(toggleButton)
        rootOverlay?.addView(statusText)

        val layoutType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        else
            @Suppress("DEPRECATION") WindowManager.LayoutParams.TYPE_PHONE

        params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            layoutType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 100
            y = 300
        }

        windowManager.addView(rootOverlay, params)

        // Make the floating bar draggable
        rootOverlay?.setOnTouchListener(object : View.OnTouchListener {
            private var initialX = 0
            private var initialY = 0
            private var initialTouchX = 0f
            private var initialTouchY = 0f

            override fun onTouch(v: View, event: MotionEvent): Boolean {
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        initialX = params.x
                        initialY = params.y
                        initialTouchX = event.rawX
                        initialTouchY = event.rawY
                        return true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        params.x = (initialX + (event.rawX - initialTouchX)).toInt()
                        params.y = (initialY + (event.rawY - initialTouchY)).toInt()
                        rootOverlay?.let { windowManager.updateViewLayout(it, params) }
                        return true
                    }
                    MotionEvent.ACTION_UP -> {
                        val dx = abs(event.rawX - initialTouchX)
                        val dy = abs(event.rawY - initialTouchY)
                        if (dx < 10 && dy < 10) {
                            v.performClick()
                        }
                        return true
                    }
                }
                return false
            }
        })
    }

    private fun removeOverlay() {
        rootOverlay?.let {
            stopBlinkDetection()
            isDetectionEnabled = false
            try {
                windowManager.removeView(it)
            } catch (_: Exception) {}
            rootOverlay = null
        }
    }

    private fun toggleDetection() {
        isDetectionEnabled = !isDetectionEnabled
        if (isDetectionEnabled) {
            toggleButton.text = "■ Stop" 
            toggleButton.setTextColor(Color.RED)
            statusText.text = "Scanning..."
            startBlinkDetection()
        } else {
            toggleButton.text = "▶ Start"
            toggleButton.setTextColor(Color.GREEN)
            statusText.text = "LazyScroll: Off"
            stopBlinkDetection()
        }
    }

    private var cameraProvider: ProcessCameraProvider? = null

    private fun startBlinkDetection() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            cameraProvider = cameraProviderFuture.get()
            val options = FaceDetectorOptions.Builder()
                .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
                .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
                .build()
            val detector = FaceDetection.getClient(options)
            val imageAnalysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
            imageAnalysis.setAnalyzer(cameraExecutor) { imageProxy ->
                if (isDetectionEnabled) {
                    processImageProxy(detector, imageProxy)
                } else {
                    imageProxy.close()
                }
            }
            val cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA
            try {
                cameraProvider?.unbindAll()
                cameraProvider?.bindToLifecycle(this, cameraSelector, imageAnalysis)
            } catch (_: Exception) {
                statusText.post { statusText.text = "Camera Error" }
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun stopBlinkDetection() {
        cameraProvider?.unbindAll()
    }

    @androidx.annotation.OptIn(androidx.camera.core.ExperimentalGetImage::class)
    private fun processImageProxy(detector: com.google.mlkit.vision.face.FaceDetector, imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        if (mediaImage != null) {
            val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
            detector.process(image)
                .addOnSuccessListener { faces ->
                    if (faces.isEmpty()) {
                        statusText.post { statusText.text = "No Face Detected" }
                    } else {
                        for (face in faces) {
                            val leftEye = face.leftEyeOpenProbability ?: 1.0f
                            val rightEye = face.rightEyeOpenProbability ?: 1.0f

                            if (leftEye < blinkThreshold && rightEye < blinkThreshold) {
                                if (!isBlinking) {
                                    isBlinking = true
                                    handleBlink()
                                }
                            } else if (leftEye > 0.6f && rightEye > 0.6f) {
                                isBlinking = false
                                if (isDetectionEnabled) {
                                    statusText.post { statusText.text = "Watching..." }
                                }
                            }
                        }
                    }
                }
                .addOnCompleteListener {
                    imageProxy.close()
                }
        } else {
            imageProxy.close()
        }
    }

    private fun handleBlink() {
        val currentTime = System.currentTimeMillis()
        val timeSinceLastBlink = currentTime - lastBlinkTime
        
        if (timeSinceLastBlink < doubleBlinkInterval && lastBlinkTime != 0L) {
            // Success: Double blink detected within 1.5 seconds
            statusText.post { 
                statusText.text = "Double Blink! Scrolling..."
                statusText.setTextColor(Color.YELLOW)
            }
            performScroll()
            lastBlinkTime = 0 
        } else {
            // First blink recorded
            lastBlinkTime = currentTime
            statusText.post { 
                statusText.text = "Blink 1 Detected..." 
                statusText.setTextColor(Color.WHITE)
            }
        }
    }

    private fun performScroll() {
        val displayMetrics = resources.displayMetrics
        val screenHeight = displayMetrics.heightPixels.toFloat()
        val screenWidth = displayMetrics.widthPixels.toFloat()
        val path = Path().apply {
            moveTo(screenWidth / 2f, screenHeight * 0.7f)
            lineTo(screenWidth / 2f, screenHeight * 0.3f)
        }
        val gestureBuilder = GestureDescription.Builder()
        gestureBuilder.addStroke(GestureDescription.StrokeDescription(path, 0, 400))
        dispatchGesture(gestureBuilder.build(), null, null)
    }

    override fun onAccessibilityEvent(event: android.view.accessibility.AccessibilityEvent?) {}
    override fun onInterrupt() {}

    override fun onDestroy() {
        super.onDestroy()
        lifecycleRegistry.currentState = Lifecycle.State.DESTROYED
        cameraExecutor.shutdown()
        removeOverlay()
    }
}
