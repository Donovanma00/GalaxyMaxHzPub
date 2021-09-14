package com.tribalfs.gmh.netspeed

import android.annotation.SuppressLint
import android.app.*
import android.app.PendingIntent.FLAG_IMMUTABLE
import android.content.Intent
import android.content.IntentFilter
import android.graphics.*
import android.graphics.drawable.Icon
import android.net.TrafficStats
import android.os.*
import android.util.Log
import android.widget.RemoteViews
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationManagerCompat
import com.tribalfs.gmh.AccessibilityPermission
import com.tribalfs.gmh.GalaxyMaxHzAccess
import com.tribalfs.gmh.R
import com.tribalfs.gmh.callbacks.ChangedStatusCallback
import com.tribalfs.gmh.helpers.CacheSettings.isOnePlus
import com.tribalfs.gmh.helpers.CacheSettings.isScreenOn
import com.tribalfs.gmh.helpers.UtilsSettingsIntents.dataUsageSettingsIntent
import com.tribalfs.gmh.receivers.ScreenStatusReceiverBasic
import com.tribalfs.gmh.sharedprefs.UtilsPrefsGmh
import com.tribalfs.gmh.sharedprefs.UtilsPrefsGmh.Companion.BIT_PER_SEC
import com.tribalfs.gmh.sharedprefs.UtilsPrefsGmh.Companion.TOTAL_SPEED
import kotlinx.coroutines.*
import java.lang.Float.min
import java.lang.String.format
import java.util.*
import kotlin.coroutines.CoroutineContext


@ExperimentalCoroutinesApi
class NetSpeedService : Service(), CoroutineScope {

    companion object {
        const val CHANNEL_ID_NET_SPEED = "NSI"
        private const val TAG = "NetSpeedService"
        private const val NOTIFICATION_ID_NET_SPEED = 7
        private const val CHANNEL_NAME_NET_SPEED = "Net Speed Indicator"
        private const val UPDATE_INTERVAL = 500L
    }
    private val mNotificationContentView: RemoteViews by lazy {RemoteViews(applicationContext.packageName, R.layout.view_indicator_notification) }
    private val notificationManagerCompat by lazy{NotificationManagerCompat.from(applicationContext)}
    private val mUtilsPrefsGmh by lazy{ UtilsPrefsGmh(applicationContext) }
    private lateinit var notificationBuilderInstance: Notification.Builder
    private lateinit var mIconSpeedPaint: Paint
    private lateinit var mIconUnitPaint:Paint
    private lateinit var mIconBitmap: Bitmap
    private lateinit var mIconCanvas: Canvas
    private var mSpeedToShow = TOTAL_SPEED
    private var mLastRxBytes: Long = 0
    private var mLastTxBytes: Long = 0
    private var mLastTime: Long = 0
    private var mOlderRxBytes: Long = 0
    private var mOlderTxBytes: Long = 0
    private var mOlderTime: Long = 0
    private var mPrevUsedHBTx = 0L
    private var mPrevUsedHBRx = 0L

    private val job = SupervisorJob()
    override val coroutineContext: CoroutineContext
        get() = job + Dispatchers.Default


    private var netstatUpdaterJob: Job? = null
    private var continueRrUpdate: Boolean = false

    private fun updateNetstat() = launch {
        while (continueRrUpdate){
            netstatUpdaterJob?.cancel()
            netstatUpdaterJob = null
            netstatUpdaterJob = launch {
                val currentRxBytes: Long = TrafficStats.getTotalRxBytes()
                val currentTxBytes: Long = TrafficStats.getTotalTxBytes()
                val currentTime = System.currentTimeMillis()

                if (mOlderTime > 0L) {
                    val usedRxdHBytes = ((currentRxBytes - mOlderRxBytes) / 100)
                    val usedTxdHBytes = ((currentTxBytes - mOlderTxBytes) / 100)
                    val usedTime = currentTime - mOlderTime
                    if (mPrevUsedHBRx != usedRxdHBytes || mPrevUsedHBTx != usedTxdHBytes) {
                        mPrevUsedHBRx = usedRxdHBytes; mPrevUsedHBTx = usedTxdHBytes
                        SpeedTypes.instance(this@NetSpeedService)
                            .processSpeed(usedTime / 1000, usedRxdHBytes, usedTxdHBytes)
                        updateNotification()
                    }
                }
                mOlderRxBytes = mLastRxBytes
                mOlderTxBytes = mLastTxBytes
                mOlderTime = mLastTime
                mLastRxBytes = currentRxBytes
                mLastTxBytes = currentTxBytes
                mLastTime = currentTime
            }
            netstatUpdaterJob?.start()

            delay(UPDATE_INTERVAL)
        }
    }

    private fun startNetStatInternal(){
        netstatUpdaterJob?.cancel()
        continueRrUpdate = true
        updateNetstat()
    }


    private fun stopNetStatInternal(){
        continueRrUpdate = false
        netstatUpdaterJob?.cancel()
    }

    private val mScreenStatusReceiver by lazy{
        ScreenStatusReceiverBasic(
            object : ChangedStatusCallback {
                @RequiresApi(Build.VERSION_CODES.M)
                override fun onChange(result: Any) {
                    //Log.d(TAG, "mScreenStatusReceiver called: $isOn")
                    isScreenOn = result as Boolean
                    if (result) {
                        startNetStatInternal()
                        notificationBuilderInstance.setVisibility(Notification.VISIBILITY_PRIVATE)
                        notificationManagerCompat.notify(
                            NOTIFICATION_ID_NET_SPEED,
                            notificationBuilderInstance.build()
                        )
                    } else {
                        Handler(Looper.getMainLooper()).postDelayed({
                            if (!isScreenOn) {
                                stopNetStatInternal()
                                notificationBuilderInstance.setVisibility(Notification.VISIBILITY_SECRET)
                                notificationManagerCompat.notify(
                                    NOTIFICATION_ID_NET_SPEED,
                                    notificationBuilderInstance.build()
                                )
                            }}, 8000)
                    }

                }
            })
    }


    private fun setupIndicatorIconGenerator() {
        mIconSpeedPaint = Paint()
        mIconSpeedPaint.color = Color.WHITE
        mIconSpeedPaint.isAntiAlias = true
        mIconSpeedPaint.textAlign = Paint.Align.CENTER
        mIconSpeedPaint.typeface = Typeface.create("sans-serif-condensed", Typeface.BOLD)
        mIconUnitPaint = Paint()
        mIconUnitPaint.color = Color.WHITE
        mIconUnitPaint.isAntiAlias = true
        mIconUnitPaint.textAlign = Paint.Align.CENTER
        mIconUnitPaint.typeface = Typeface.create("sans-serif-condensed", Typeface.BOLD)
        mIconBitmap = Bitmap.createBitmap(96, 96, Bitmap.Config.ARGB_8888)
        mIconCanvas = Canvas(mIconBitmap)
    }

    @ExperimentalCoroutinesApi
    private fun setupScreenStatusReceiver(){
        if (!AccessibilityPermission.isAccessibilityEnabled(applicationContext,GalaxyMaxHzAccess::class.java)) {
            IntentFilter().let {
                it.addAction(Intent.ACTION_SCREEN_OFF)
                it.addAction(Intent.ACTION_SCREEN_ON)
                it.priority = 999
                registerReceiver(mScreenStatusReceiver, it)
            }
        }
    }

    @SuppressLint("NewApi")
    private fun setupNotification() {
        //create notification channel
        (NotificationChannel(
            CHANNEL_ID_NET_SPEED,
            CHANNEL_NAME_NET_SPEED,
            NotificationManager.IMPORTANCE_LOW
        )).apply {
            lightColor = Color.BLUE
            lockscreenVisibility = Notification.VISIBILITY_SECRET
            setShowBadge(false)
            vibrationPattern = longArrayOf(0)
            enableVibration(true)
            setAllowBubbles(false)
            notificationManagerCompat.createNotificationChannel(this)
        }


        val pendingIntent: PendingIntent = PendingIntent.getActivity(applicationContext, 0, dataUsageSettingsIntent, FLAG_IMMUTABLE)

        notificationBuilderInstance = (Notification.Builder(applicationContext,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) CHANNEL_ID_NET_SPEED else ""))
            .apply {
                setSmallIcon(R.drawable.ic_baseline_speed_12)
                setOngoing(true)
                setOnlyAlertOnce(true)
                setCategory(Notification.CATEGORY_SERVICE)
                setVisibility(Notification.VISIBILITY_PUBLIC)
                setLocalOnly(true)
                if (!isOnePlus) {setContentIntent(pendingIntent)}
                setAutoCancel(false)
                setCustomContentView(mNotificationContentView)
            }
    }


    @SuppressLint("NewApi")
    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "onCreate() called")
        setupIndicatorIconGenerator()
        setupNotification()
        setupScreenStatusReceiver()
    }


    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {//keep intent nullable for system restart
        Log.d(TAG, "onStartCommand() called")
        handleConfigChange()
        startForeground(NOTIFICATION_ID_NET_SPEED, notificationBuilderInstance.build())
        startNetStatInternal()
        return START_STICKY
    }


    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun onDestroy() {
        stopNetStatInternal()
        notificationManagerCompat.cancel(NOTIFICATION_ID_NET_SPEED)
        try {
            unregisterReceiver(mScreenStatusReceiver)
        }catch(_: java.lang.Exception){}
        stopForeground(true)
        job.cancel()
        super.onDestroy()
    }

    @SuppressLint("NewApi")
    private suspend fun updateNotification() = withContext(Dispatchers.Main) {
        notificationBuilderInstance.apply {
            SpeedTypes.instance(applicationContext).let { speedTypes ->
                speedTypes.getSpeedDetails(mSpeedToShow).let { speedDetails ->
                    setSmallIcon(
                        getIndicatorIcon(
                            speedDetails.speedValue,
                            speedDetails.speedUnit
                        )
                    )
                }
                setCustomContentView(RemoteViews(mNotificationContentView).apply {
                    setTextViewText(
                        R.id.notificationTextDl,
                        format(
                            Locale.ENGLISH,
                            applicationContext.getString(R.string.notif_sp_dl),
                            speedTypes.dlSpeedData.speedValue,
                            speedTypes.dlSpeedData.speedUnit
                        )
                    )
                    setTextViewText(
                        R.id.notificationTextUl,
                        format(
                            Locale.ENGLISH,
                            applicationContext.getString(R.string.notif_sp_ul),
                            speedTypes.upSpeedData.speedValue,
                            speedTypes.upSpeedData.speedUnit
                        )
                    )
                    setTextViewText(
                        R.id.notificationTextTot,
                        format(
                            Locale.ENGLISH,
                            applicationContext.getString(R.string.notif_sp_cb),
                            speedTypes.tlSpeedData.speedValue,
                            speedTypes.tlSpeedData.speedUnit
                        )
                    )
                }
                )
            }
            notificationManagerCompat.notify(
                NOTIFICATION_ID_NET_SPEED, build())
        }
    }

    @RequiresApi(Build.VERSION_CODES.N)
    private suspend fun getIndicatorIcon(speedValue: String, speedUnit: String): Icon? = withContext(Dispatchers.Default){
        mIconSpeedPaint.textSize = 72f
        mIconUnitPaint.textSize = 50f
        mIconSpeedPaint.textSize = min(72*96/mIconSpeedPaint.measureText(speedValue),72f)
        mIconUnitPaint.textSize = min(50*96/mIconUnitPaint.measureText(speedUnit),50f)
        mIconCanvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)
        mIconCanvas.drawText(speedValue, 48f, 50f, mIconSpeedPaint)
        mIconCanvas.drawText(speedUnit, 48f, 92f, mIconUnitPaint)
        return@withContext Icon.createWithBitmap(mIconBitmap)
    }



    private fun handleConfigChange() {
        SpeedTypes.instance(applicationContext).setIsSpeedUnitBits(mUtilsPrefsGmh.gmhPrefSpeedUnit == BIT_PER_SEC)
        mSpeedToShow = mUtilsPrefsGmh.gmhPrefSpeedToShow
    }


}