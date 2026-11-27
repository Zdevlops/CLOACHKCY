package com.zdevlops.cloachy.monitor

import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.util.TypedValue
import android.view.Gravity
import android.view.MotionEvent
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.TextView
import com.zdevlops.cloachy.model.OverlayPosition
import kotlinx.coroutines.*

class PerformanceOverlayService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var windowManager: WindowManager? = null
    private var rootContainer: LinearLayout? = null
    private var fpsTextView: TextView? = null
    private var cpuTextView: TextView? = null
    private var gpuTextView: TextView? = null

    override fun onCreate() {
        super.onCreate()
        if (!Settings.canDrawOverlays(this)) {
            stopSelf()
            return
        }
        setupOverlayWindow()
        startTelemetryLoop()
    }

    private fun dp(value: Float): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            value,
            resources.displayMetrics
        ).toInt()
    }

    private fun setupOverlayWindow() {
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val prefs = getSharedPreferences("cloachy_overlay_prefs", Context.MODE_PRIVATE)
        val posOrdinal = prefs.getInt("overlay_position", OverlayPosition.TOP_LEFT.ordinal)
        val alphaPct = prefs.getInt("overlay_opacity", 82).coerceIn(35, 100)
        val position = OverlayPosition.values().getOrElse(posOrdinal) { OverlayPosition.TOP_LEFT }

        val alphaByte = ((alphaPct / 100f) * 255).toInt().coerceIn(90, 245)
        val bgDrawable = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = dp(8f).toFloat()
            setColor(Color.argb(alphaByte, 13, 17, 23)) // #CC0D1117 bersih & netral
            setStroke(dp(1f), Color.argb(85, 56, 189, 248)) // Garis tepi biru es redup
        }

        val container = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            background = bgDrawable
            setPadding(dp(9f), dp(5f), dp(9f), dp(5f))
        }

        fun makeMetricText(initial: String, textColor: Int): TextView {
            return TextView(this).apply {
                text = initial
                setTextColor(textColor)
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
                typeface = Typeface.MONOSPACE
                setPadding(dp(4f), 0, dp(4f), 0)
            }
        }

        fun makeSeparator(): TextView {
            return TextView(this).apply {
                text = "·"
                setTextColor(Color.argb(120, 148, 163, 184))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
                typeface = Typeface.MONOSPACE
            }
        }

        val tvFps = makeMetricText("FPS --", Color.rgb(241, 245, 249))
        val tvCpu = makeMetricText("CPU --%", Color.rgb(186, 230, 253))
        val tvGpu = makeMetricText("GPU --%", Color.rgb(125, 211, 252))

        container.addView(tvFps)
        container.addView(makeSeparator())
        container.addView(tvCpu)
        container.addView(makeSeparator())
        container.addView(tvGpu)

        val layoutType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            layoutType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = if (position == OverlayPosition.TOP_RIGHT) {
                Gravity.TOP or Gravity.END
            } else {
                Gravity.TOP or Gravity.START
            }
            x = dp(12f)
            y = dp(34f)
        }

        // Bisa digeser bebas oleh user kalau menutupi tombol in-game
        var initialX = 0
        var initialY = 0
        var initialTouchX = 0f
        var initialTouchY = 0f
        container.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = params.x
                    initialY = params.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = (event.rawX - initialTouchX).toInt()
                    val dy = (event.rawY - initialTouchY).toInt()
                    params.x = if (position == OverlayPosition.TOP_RIGHT) initialX - dx else initialX + dx
                    params.y = initialY + dy
                    windowManager?.updateViewLayout(container, params)
                    true
                }
                else -> false
            }
        }

        windowManager?.addView(container, params)
        rootContainer = container
        fpsTextView = tvFps
        cpuTextView = tvCpu
        gpuTextView = tvGpu
    }

    private fun startTelemetryLoop() {
        scope.launch {
            while (isActive) {
                val state = withContext(Dispatchers.IO) {
                    SystemTelemetryMonitor.sampleLiveTelemetry(this@PerformanceOverlayService, null)
                }
                val fpsLabel = if (state.fps > 0) "FPS ${state.fps}" else "FPS --"
                fpsTextView?.text = fpsLabel
                cpuTextView?.text = "CPU ${state.cpuLoadPercent}%"
                gpuTextView?.text = "GPU ${state.gpuLoadPercent}%"
                delay(1000L)
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onDestroy() {
        scope.cancel()
        try {
            rootContainer?.let { windowManager?.removeView(it) }
        } catch (_: Exception) {
        }
        rootContainer = null
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
