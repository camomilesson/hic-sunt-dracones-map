package com.andrei.dracones.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.graphics.drawable.Icon
import android.os.Build
import android.os.IBinder
import android.util.Log
import com.andrei.dracones.MainActivity
import com.andrei.dracones.data.location.LocationTracker
import com.andrei.dracones.data.persistence.AppDatabase
import com.andrei.dracones.data.repository.ExplorationRepository
import com.andrei.dracones.domain.diagnostics.CrashReporter
import com.andrei.dracones.domain.h3.H3Manager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ExplorationTrackingService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private var locationTracker: LocationTracker? = null
    private var repository: ExplorationRepository? = null
    private var locationJob: Job? = null

    private val recentH3Cells = ArrayDeque<String>(3)

    private val stopReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == ACTION_STOP_TRACKING) {
                stopTracking()
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        val database = AppDatabase.getDatabase(applicationContext)
        repository = ExplorationRepository(database.visitedCellDao())
        locationTracker = LocationTracker(applicationContext)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(stopReceiver, IntentFilter(ACTION_STOP_TRACKING), RECEIVER_NOT_EXPORTED)
        } else {
            @Suppress("UnspecifiedRegisterReceiverFlag")
            registerReceiver(stopReceiver, IntentFilter(ACTION_STOP_TRACKING))
        }
        _isTracking.value = true
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP_TRACKING) {
            stopTracking()
            return START_NOT_STICKY
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
                NOTIFICATION_ID,
                buildNotification(),
                ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
            )
        } else {
            startForeground(NOTIFICATION_ID, buildNotification())
        }
        startLocationCollection()
        CrashReporter.log("Exploration tracking service started")
        return START_STICKY
    }

    private fun startLocationCollection() {
        if (locationJob != null) return

        val tracker = locationTracker
        val repo = repository

        if (tracker == null) {
            Log.e(TAG, "Service: locationTracker is null")
            return
        }

        if (repo == null) {
            Log.e(TAG, "Service: repository is null")
            return
        }

        locationJob = tracker.getLocationUpdates(5_000L)
            .onEach { latLng ->
                serviceScope.launch {
                    try {
                        val h3Index = withContext(Dispatchers.Default) {
                            H3Manager.latLngToCell(latLng)
                        }

                        val alreadyInRecent = synchronized(recentH3Cells) {
                            recentH3Cells.contains(h3Index)
                        }

                        if (!alreadyInRecent) {
                            repo.markCellVisited(h3Index)

                            synchronized(recentH3Cells) {
                                if (recentH3Cells.size >= RECENT_CELL_BUFFER_SIZE) {
                                    recentH3Cells.removeFirst()
                                }
                                recentH3Cells.addLast(h3Index)
                            }
                        }
                    } catch (e: Exception) {
                        CrashReporter.recordException(e)
                        Log.e(TAG, "Service: location update processing failed", e)
                    }
                }
            }
            .launchIn(serviceScope)
    }

    private fun stopTracking() {
        _isTracking.value = false
        locationJob?.cancel()
        locationJob = null

        stopForeground(STOP_FOREGROUND_REMOVE)
        val manager = getSystemService(NotificationManager::class.java)
        manager.cancel(NOTIFICATION_ID)
        stopSelf()
    }

    private fun buildNotification(): Notification {
        createNotificationChannel()

        val stopIntent = Intent(ACTION_STOP_TRACKING).apply {
            `package` = packageName
        }

        val stopPendingIntent = PendingIntent.getBroadcast(
            this,
            STOP_REQUEST_CODE,
            stopIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val contentPendingIntent = PendingIntent.getActivity(
            this,
            CONTENT_REQUEST_CODE,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val stopAction = Notification.Action.Builder(
            Icon.createWithResource(
                this,
                android.R.drawable.ic_menu_close_clear_cancel
            ),
            "Stop",
            stopPendingIntent
        ).build()

        return Notification.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle("Hic Sunt Dracones")
            .setContentText("Background tracking active, you can close the app")
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setContentIntent(contentPendingIntent)
            .setOngoing(true)
            .setAutoCancel(false)
            .setOnlyAlertOnce(true)
            .setDeleteIntent(stopPendingIntent)
            .addAction(stopAction)
            .build()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            NOTIFICATION_CHANNEL_ID,
            "Exploration Tracking",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Shows when exploration tracking is active"
            setShowBadge(true)
        }

        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }

    override fun onDestroy() {
        _isTracking.value = false
        CrashReporter.log("Exploration tracking service stopped")

        try {
            unregisterReceiver(stopReceiver)
        } catch (e: Exception) {
            // Already unregistered or not registered
        }

        locationJob?.cancel()
        locationJob = null
        serviceScope.cancel()

        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        private const val TAG = "HSD"

        private const val ACTION_STOP_TRACKING = "com.andrei.dracones.ACTION_STOP_TRACKING"

        private const val NOTIFICATION_ID = 1
        private const val NOTIFICATION_CHANNEL_ID = "exploration_tracking_channel_v3"

        private const val STOP_REQUEST_CODE = 1001
        private const val CONTENT_REQUEST_CODE = 1002

        private const val RECENT_CELL_BUFFER_SIZE = 3

        private val _isTracking = MutableStateFlow(false)
        val isTracking: StateFlow<Boolean> = _isTracking.asStateFlow()
    }
}