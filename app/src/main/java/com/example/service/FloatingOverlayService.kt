package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ComposeView
import androidx.core.app.NotificationCompat
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.example.MainActivity
import com.example.R
import com.example.data.db.TaskDatabase
import com.example.data.gemini.GeminiHomeworkClient
import com.example.data.gemini.HomeworkSolution
import com.example.data.model.TaskItem
import com.example.overlay.FloatingOverlayContent
import com.example.overlay.OverlayMode
import com.example.ui.theme.MyApplicationTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class FloatingOverlayService : Service() {

    private val serviceJob = Job()
    private val serviceScope = CoroutineScope(Dispatchers.Main + serviceJob)

    private var windowManager: WindowManager? = null
    private var composeView: ComposeView? = null
    private var layoutParams: WindowManager.LayoutParams? = null
    private var lifecycleOwner: OverlayLifecycleOwner? = null

    private lateinit var geminiClient: GeminiHomeworkClient
    private lateinit var database: TaskDatabase

    // UI state in service
    private var overlayMode by mutableStateOf(OverlayMode.MINIMIZED)
    private var currentSolution by mutableStateOf<HomeworkSolution?>(null)
    private var isLoading by mutableStateOf(false)
    private var statusMessage by mutableStateOf("")

    companion object {
        const val CHANNEL_ID = "ninja_tugas_silent_channel"
        const val NOTIFICATION_ID = 1001

        const val ACTION_START = "ACTION_START_OVERLAY"
        const val ACTION_STOP = "ACTION_STOP_OVERLAY"
        const val ACTION_STEALTH_HIDE = "ACTION_STEALTH_HIDE"
        const val ACTION_LIVE_CAPTURE = "ACTION_LIVE_CAPTURE"

        private val _isRunningFlow = MutableStateFlow(false)
        val isRunningFlow = _isRunningFlow.asStateFlow()

        fun startService(context: Context) {
            val intent = Intent(context, FloatingOverlayService::class.java).apply {
                action = ACTION_START
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stopService(context: Context) {
            val intent = Intent(context, FloatingOverlayService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        geminiClient = GeminiHomeworkClient(applicationContext)
        database = TaskDatabase.getInstance(applicationContext)
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        createSilentNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopSelf()
                return START_NOT_STICKY
            }
            ACTION_STEALTH_HIDE -> {
                overlayMode = OverlayMode.MINIMIZED
                return START_STICKY
            }
            ACTION_LIVE_CAPTURE -> {
                triggerLiveScreenCapture()
                return START_STICKY
            }
        }

        startForegroundNotification()
        showOverlayWindow()
        _isRunningFlow.value = true
        return START_STICKY
    }

    private fun createSilentNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "NinjaTugas Layanan Hening",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Layanan aktif untuk jendela mengambang NinjaTugas"
                setSound(null, null)
                enableVibration(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    private fun startForegroundNotification() {
        val openAppIntent = Intent(this, MainActivity::class.java)
        val pendingOpen = PendingIntent.getActivity(
            this,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val hideIntent = Intent(this, FloatingOverlayService::class.java).apply {
            action = ACTION_STEALTH_HIDE
        }
        val pendingHide = PendingIntent.getService(
            this,
            1,
            hideIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(this, FloatingOverlayService::class.java).apply {
            action = ACTION_STOP
        }
        val pendingStop = PendingIntent.getService(
            this,
            2,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification: Notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("NinjaTugas Aktif (100% Hening)")
            .setContentText("Jendela mengambang aktif di layar. Ketuk untuk membuka kontrol.")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setSilent(true)
            .setContentIntent(pendingOpen)
            .addAction(R.drawable.ic_launcher_foreground, "Mode Samaran", pendingHide)
            .addAction(R.drawable.ic_launcher_foreground, "Tutup", pendingStop)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                // Media projection type on Android 14+
                startForeground(
                    NOTIFICATION_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION
                )
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun showOverlayWindow() {
        if (composeView != null) return

        val owner = OverlayLifecycleOwner()
        owner.start()
        lifecycleOwner = owner

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 80
            y = 200
        }
        layoutParams = params

        val view = ComposeView(this).apply {
            setViewTreeLifecycleOwner(owner)
            setViewTreeViewModelStoreOwner(owner)
            setViewTreeSavedStateRegistryOwner(owner)

            setContent {
                MyApplicationTheme(darkTheme = true) {
                    FloatingOverlayContent(
                        mode = overlayMode,
                        solution = currentSolution,
                        isLoading = isLoading,
                        statusMessage = statusMessage,
                        onModeChange = { newMode ->
                            overlayMode = newMode
                            updateWindowFocusable(newMode)
                        },
                        onLiveScreenCapture = { triggerLiveScreenCapture() },
                        onSolveQuestion = { question -> solveQuestionText(question) },
                        onCloseService = { stopSelf() },
                        onDragDelta = { dx, dy ->
                            this@FloatingOverlayService.layoutParams?.let { lp ->
                                lp.x += dx.toInt()
                                lp.y += dy.toInt()
                                windowManager?.updateViewLayout(this@apply, lp)
                            }
                        }
                    )
                }
            }
        }

        composeView = view
        try {
            windowManager?.addView(view, params)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun updateWindowFocusable(mode: OverlayMode) {
        val lp = layoutParams ?: return
        val view = composeView ?: return

        // If in expanded mode and user needs to type, adjust flags
        if (mode == OverlayMode.EXPANDED) {
            lp.flags = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH
        } else {
            lp.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH
        }
        try {
            windowManager?.updateViewLayout(view, lp)
        } catch (ignored: Exception) {
        }
    }

    private fun triggerLiveScreenCapture() {
        if (isLoading) return
        isLoading = true
        statusMessage = "Menyiapkan tangkapan layar..."

        serviceScope.launch {
            // Temporarily hide overlay so it does not capture itself
            composeView?.visibility = View.INVISIBLE
            delay(120) // Brief delay to let screen frame settle

            val screenBitmap = ScreenCaptureHolder.captureScreen(applicationContext)

            // Re-show overlay
            composeView?.visibility = View.VISIBLE

            if (screenBitmap == null) {
                isLoading = false
                statusMessage = "Tangkapan layar gagal. Pastikan izin Live Screen aktif."
                // Expand so user can see message
                overlayMode = OverlayMode.EXPANDED
                return@launch
            }

            statusMessage = "Menganalisis soal di layar..."
            overlayMode = OverlayMode.EXPANDED

            val result = geminiClient.solveHomework(
                questionText = null,
                imageBitmap = screenBitmap
            )

            isLoading = false
            result.onSuccess { sol ->
                currentSolution = sol
                statusMessage = ""
                // Save to history
                serviceScope.launch(Dispatchers.IO) {
                    database.taskDao().insertTask(
                        TaskItem(
                            subject = sol.subject,
                            questionText = "Analisis Layar Langsung",
                            shortAnswer = sol.shortAnswer,
                            detailedSteps = sol.detailedSteps,
                            keyFormulas = sol.keyFormulas,
                            source = "LIVE_SCREEN"
                        )
                    )
                }
            }.onFailure { err ->
                statusMessage = "Error: ${err.message}"
            }
        }
    }

    private fun solveQuestionText(question: String) {
        if (isLoading) return
        isLoading = true
        statusMessage = "Memecahkan tugas..."

        serviceScope.launch {
            val result = geminiClient.solveHomework(
                questionText = question,
                imageBitmap = null
            )

            isLoading = false
            result.onSuccess { sol ->
                currentSolution = sol
                statusMessage = ""
                // Save to history
                serviceScope.launch(Dispatchers.IO) {
                    database.taskDao().insertTask(
                        TaskItem(
                            subject = sol.subject,
                            questionText = question,
                            shortAnswer = sol.shortAnswer,
                            detailedSteps = sol.detailedSteps,
                            keyFormulas = sol.keyFormulas,
                            source = "TEXT"
                        )
                    )
                }
            }.onFailure { err ->
                statusMessage = "Error: ${err.message}"
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        _isRunningFlow.value = false
        serviceJob.cancel()
        lifecycleOwner?.destroy()

        composeView?.let { view ->
            try {
                windowManager?.removeView(view)
            } catch (ignored: Exception) {
            }
        }
        composeView = null
    }
}
