package com.example.testboundservice

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import com.example.testboundservice.Constants.ACTION_PAUSE_SERVICE
import com.example.testboundservice.Constants.ACTION_START_OR_RESUME_SERVICE
import com.example.testboundservice.Constants.ACTION_STOP_SERVICE
import com.example.testboundservice.Constants.LOG_SERVICE
import com.example.testboundservice.Constants.NOTIFICATION_CHANNEL_ID
import com.example.testboundservice.Constants.NOTIFICATION_CHANNEL_NAME
import com.example.testboundservice.Constants.NOTIFICATION_ID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import java.util.*


//every service needs to be registered in the manifest
class MyService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private lateinit var notificationManager: NotificationManager
    private lateinit var mBinder: IBinder

    // Random number generator
    private lateinit var mGenerator: Random

    private val _numberGenerated = MutableSharedFlow<Int>(replay = 1)
    val numberGenerated = _numberGenerated.asSharedFlow()

    private var isActive = false

    override fun onCreate() {
        super.onCreate()
        mGenerator = Random()
        mBinder = MyBinder()
    }

    companion object {
        val TAG = "MyService"
    }

    /**
     * Class used for the client Binder. The Binder object is responsible for returning an instance
     * of "MyService" to the client.
     */
    inner class MyBinder : Binder() {
        fun getService(): MyService {
            return this@MyService
        }
    }

    /**
     * This is how the client gets the IBinder object from the service. It's retrieve by the "ServiceConnection"
     * which you'll see later.
     **/
    override fun onBind(p0: Intent?): IBinder? {
        return mBinder
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.let {
            when (it.action) {
                ACTION_START_OR_RESUME_SERVICE -> {
                    if (!isActive) {
                        Log.d(LOG_SERVICE, "ACTION_START")
                        isActive = true
                        startOurForegroundService()
                    } else {
                        Log.d(LOG_SERVICE, "ACTION_RESUME")
                    }

                }
                ACTION_PAUSE_SERVICE -> {
                    Log.d(LOG_SERVICE, "ACTION_PAUSE_SERVICE")
                }
                ACTION_STOP_SERVICE -> {
                    Log.d(LOG_SERVICE, "ACTION_STOP_SERVICE")
                    stop()
                }
                else -> {}
            }
        }
        return super.onStartCommand(intent, flags, startId)
    }

    /** method for clients to get a random number from 0 - 100  */
    private fun getRandomNumber() {
        serviceScope.launch {
            while (isActive) {
                val generatedOne = mGenerator.nextInt(100)
                Log.d(LOG_SERVICE, "getRandomNumber: Launched $generatedOne")
                updateNotification(generatedOne)
                _numberGenerated.emit(generatedOne)
                delay(10000)
            }
        }
    }

    private fun updateNotification(newValue: Int) {

        val notification =
            NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)// our ID greater than 0
                .setAutoCancel(false) // can't be swiped
                .setOngoing(true)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle("My service")
                .setContentIntent(getMainActivityPendingIntent()) // the pending intent to execute when clicked the notification

        val updatedNotification = notification.setContentText(
            "newValue: ($newValue)"
        )
        // we use the same id in order to update the notification and not create another
        notificationManager.notify(NOTIFICATION_ID, updatedNotification.build())
    }

    private fun startOurForegroundService() {
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE)
                as NotificationManager

        createNotificationChannel(notificationManager)

        val notificationBuilder =
            NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)// our ID greater than 0
                .setAutoCancel(false) // can't be swiped
                .setOngoing(true)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle("My service")
                .setContentText("Service running") // data before update
                .setContentIntent(getMainActivityPendingIntent()) // the pending intent to execute when clicked the notification

        //https://stackoverflow.com/questions/72664186/why-did-android-13-remove-the-foreground-service-notification

        startForeground(NOTIFICATION_ID, notificationBuilder.build())
        getRandomNumber()
    }


    /**
    *     android:launchMode="singleTask" in manifest to not allow multiple mainActivities
    * */
    private fun getMainActivityPendingIntent(): PendingIntent = PendingIntent.getActivity(
        this,
        0,
        Intent(this, MainActivity::class.java),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
    )

    private fun createNotificationChannel(notificationManager: NotificationManager) {
        val channel = NotificationChannel(
            NOTIFICATION_CHANNEL_ID, // notification ID always has to be 1 or greater but not 0
            NOTIFICATION_CHANNEL_NAME,
            NotificationManager.IMPORTANCE_LOW
        )
        notificationManager.createNotificationChannel(channel)
    }


    private fun stop() {
        isActive = false
        stopForeground(STOP_FOREGROUND_REMOVE) // use the function from the parent class
        stopSelf()
    }

    override fun onDestroy() {
        super.onDestroy()
        // we cancel our scope when the service is destroyed
        serviceScope.cancel()
    }

}