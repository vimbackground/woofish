package woofish

import android.app.*
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat

class MediaNotificationService : Service() {

    companion object {
        const val CHANNEL_ID = "wooden_fish_bgm_channel"
        const val NOTIFICATION_ID = 1001

        const val ACTION_TOGGLE_BGM = "woofish.action.TOGGLE_BGM"
        const val ACTION_TOGGLE_ANIM = "woofish.action.TOGGLE_ANIM"
        const val ACTION_TOGGLE_SOUND = "woofish.action.TOGGLE_SOUND"
        const val ACTION_UPDATE_NOTIFICATION = "woofish.action.UPDATE_NOTIFICATION"

        fun updateService(context: Context) {
            val intent = Intent(context, MediaNotificationService::class.java).apply {
                action = ACTION_UPDATE_NOTIFICATION
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification = buildLockScreenNotification()
        startForeground(NOTIFICATION_ID, notification)
        return START_STICKY
    }

    private fun buildLockScreenNotification(): Notification {
        val prefs = getSharedPreferences("wooden_fish_prefs", Context.MODE_PRIVATE)
        val isBgmPlaying = prefs.getBoolean("key_bgm_playing", false)
        val isAnimEnabled = prefs.getBoolean("key_animation_enabled", true)
        val soundIndex = prefs.getInt("key_sound_index", 0)
        val count = prefs.getLong("key_count", 0L)

        // 1. 点击通知打开主界面
        val contentIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // 2. 锁屏动作：开关 BGM
        val bgmIntent = PendingIntent.getBroadcast(
            this,
            1,
            Intent(this, MediaActionReceiver::class.java).apply { action = ACTION_TOGGLE_BGM },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // 3. 锁屏动作：开关木鱼动效
        val animIntent = PendingIntent.getBroadcast(
            this,
            2,
            Intent(this, MediaActionReceiver::class.java).apply { action = ACTION_TOGGLE_ANIM },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // 4. 锁屏动作：切换敲击音效
        val soundIntent = PendingIntent.getBroadcast(
            this,
            3,
            Intent(this, MediaActionReceiver::class.java).apply { action = ACTION_TOGGLE_SOUND },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val bgmTitle = if (isBgmPlaying) "⏸️ 暂停音乐" else "▶️ 播放音乐"
        val animTitle = if (isAnimEnabled) "✨ 动效:开" else "🌑 动效:关"
        val soundTitle = if (soundIndex == 0) "🔊 音效 1" else "🔊 音效 2"

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_wooden_fish)
            .setContentTitle("电子木鱼 · 沉浸修行中")
            .setContentText("功德: $count | $animTitle | $soundTitle")
            .setContentIntent(contentIntent)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC) // 息屏/锁屏公开显示控制
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .addAction(0, bgmTitle, bgmIntent)
            .addAction(0, animTitle, animIntent)
            .addAction(0, soundTitle, soundIntent)
            .setStyle(
                androidx.media.app.NotificationCompat.MediaStyle()
                    .setShowActionsInCompactView(0, 1, 2)
            )
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "电子木鱼背景服务",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "支持息屏状态下控制背景音乐与动效切换"
                setShowBadge(false)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }
}

class MediaActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val prefs = context.getSharedPreferences("wooden_fish_prefs", Context.MODE_PRIVATE)
        when (intent?.action) {
            MediaNotificationService.ACTION_TOGGLE_BGM -> {
                // 发送广播给 MainViewModel 或全局单例处理
                val current = prefs.getBoolean("key_bgm_playing", false)
                prefs.edit().putBoolean("key_bgm_playing", !current).apply()
                MainViewModel.instance?.toggleBgmFromLockScreen()
            }
            MediaNotificationService.ACTION_TOGGLE_ANIM -> {
                val current = prefs.getBoolean("key_animation_enabled", true)
                val newAnim = !current
                prefs.edit().putBoolean("key_animation_enabled", newAnim).apply()
                MainViewModel.instance?.setAnimationFromLockScreen(newAnim)
            }
            MediaNotificationService.ACTION_TOGGLE_SOUND -> {
                val current = prefs.getInt("key_sound_index", 0)
                val nextSound = if (current == 0) 1 else 0
                prefs.edit().putInt("key_sound_index", nextSound).apply()
                MainViewModel.instance?.setSoundFromLockScreen(nextSound)
            }
        }
        MediaNotificationService.updateService(context)
    }
}
