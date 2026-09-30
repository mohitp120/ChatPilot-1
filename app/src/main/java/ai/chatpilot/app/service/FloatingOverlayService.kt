package ai.chatpilot.app.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.ui.platform.ComposeView
import androidx.core.app.NotificationCompat
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import ai.chatpilot.app.R
import ai.chatpilot.app.ui.overlay.FloatingOverlayHostView
import ai.chatpilot.app.ui.overlay.OverlayState
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * FloatingOverlayService
 * Keeps the ChatPilot bubble visible and draggable above WhatsApp, Instagram, Telegram, etc.
 */
class FloatingOverlayService : Service() {

    private lateinit var windowManager: WindowManager
    private var overlayComposeView: ComposeView? = null
    private lateinit var layoutParams: WindowManager.LayoutParams

    private val overlayState = MutableStateFlow<OverlayState>(OverlayState.Minimized)

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        startForegroundServiceNotification()
        createFloatingOverlay()
    }

    private fun startForegroundServiceNotification() {
        val channelId = "chatpilot_overlay_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "ChatPilot Floating Assistant",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows the active floating copilot assistant"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }

        val notification: Notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("ChatPilot AI Active")
            .setContentText("Tap floating bubble above any chat app to get replies.")
            .setSmallIcon(R.drawable.ic_chatpilot_bubble)
            .setOngoing(true)
            .build()

        startForeground(1001, notification)
    }

    private fun createFloatingOverlay() {
        val overlayType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        layoutParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            overlayType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                    WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 30
            y = 300
            alpha = 0.95f // default transparency
        }

        overlayComposeView = ComposeView(this).apply {
            // Setup ViewTree owners for Jetpack Compose runtime in a Service
            FloatingOverlayHostView.setupViewTreeOwners(this)
            setContent {
                FloatingOverlayHostView(
                    service = this@FloatingOverlayService,
                    windowManager = windowManager,
                    layoutParams = layoutParams,
                    overlayState = overlayState
                )
            }
        }

        windowManager.addView(overlayComposeView, layoutParams)
    }

    fun updateOverlayPosition(newX: Int, newY: Int) {
        layoutParams.x = newX
        layoutParams.y = newY
        windowManager.updateViewLayout(overlayComposeView, layoutParams)
    }

    fun updateOverlayAlpha(alpha: Float) {
        layoutParams.alpha = alpha.coerceIn(0.2f, 1.0f)
        windowManager.updateViewLayout(overlayComposeView, layoutParams)
    }

    fun updateOverlayDimensions(width: Int, height: Int) {
        layoutParams.width = width
        layoutParams.height = height
        windowManager.updateViewLayout(overlayComposeView, layoutParams)
    }

    override fun onDestroy() {
        super.onDestroy()
        overlayComposeView?.let { windowManager.removeView(it) }
    }

    override fun onBind(intent: Intent?): IBinder? = null
}