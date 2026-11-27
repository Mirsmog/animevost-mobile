package com.animevost.app

import android.Manifest
import android.app.NotificationManager
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.animevost.app.core.data.notification.AnimeNotificationIntent
import com.animevost.app.core.data.notification.FavoriteEpisodeNotifier
import com.animevost.app.core.domain.repository.AuthRepository
import com.animevost.app.core.domain.repository.FavoriteRepository
import com.animevost.app.core.domain.repository.UserListRepository
import com.animevost.app.core.ui.theme.AnimeVostTheme
import com.animevost.app.navigation.AppNavGraph
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject lateinit var authRepository: AuthRepository
    @Inject lateinit var favoriteRepository: FavoriteRepository
    @Inject lateinit var userListRepository: UserListRepository

    private val notificationAnimeUrl = MutableStateFlow<String?>(null)

    private val mainViewModel: MainViewModel by viewModels()
    private var notificationsAllowed by mutableStateOf(false)
    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { checkNotificationChannel() }
    private val notificationSettingsLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { checkNotificationChannel() }
    private val channelSettingsLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { finishNotificationPermission() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                authRepository.isLoggedInFlow.collectLatest { loggedIn ->
                    if (!loggedIn) return@collectLatest
                    while (isActive) {
                        try {
                            userListRepository.syncFromRemote()
                        } catch (error: CancellationException) {
                            throw error
                        } catch (error: Exception) {
                            Timber.w(error, "Watch-list sync failed; will retry")
                        }
                        favoriteRepository.loadRemoteFavorites()
                        delay(60_000)
                    }
                }
            }
        }
        handleNotificationIntent(intent)
        enableEdgeToEdge()
        if (BuildConfig.DEBUG) {
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        setContent {
            val animeUrl by notificationAnimeUrl.collectAsStateWithLifecycle()
            AnimeVostTheme {
                AppNavGraph(
                    mainViewModel = mainViewModel,
                    notificationAnimeUrl = animeUrl,
                    notificationsAllowed = notificationsAllowed,
                    enableNotifications = ::enableNotifications,
                    onNotificationNavigationHandled = { consumeNotificationNavigation(animeUrl) },
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleNotificationIntent(intent)
    }

    private fun handleNotificationIntent(intent: Intent?) {
        notificationAnimeUrl.value = intent
            ?.getStringExtra(AnimeNotificationIntent.EXTRA_ANIME_URL)
            ?.takeIf(String::isNotBlank)
    }

    private fun consumeNotificationNavigation(url: String?) {
        if (notificationAnimeUrl.value != url) return
        notificationAnimeUrl.value = null
        intent?.removeExtra(AnimeNotificationIntent.EXTRA_ANIME_URL)
    }

    override fun onResume() {
        super.onResume()
        notificationsAllowed = canPostEpisodeNotifications()
        if (notificationsAllowed) mainViewModel.finishNotificationRequest(true)
    }

    private fun finishNotificationPermission() {
        notificationsAllowed = canPostEpisodeNotifications()
        mainViewModel.finishNotificationRequest(notificationsAllowed)
    }

    private fun canPostEpisodeNotifications(): Boolean {
        val manager = NotificationManagerCompat.from(this)
        return manager.areNotificationsEnabled() && !isEpisodeChannelBlocked()
    }

    private fun isEpisodeChannelBlocked(): Boolean {
        val channel = NotificationManagerCompat.from(this).getNotificationChannel(FavoriteEpisodeNotifier.CHANNEL_ID)
        return channel?.importance == NotificationManager.IMPORTANCE_NONE
    }

    private fun checkNotificationChannel() {
        if (NotificationManagerCompat.from(this).areNotificationsEnabled() && isEpisodeChannelBlocked()) {
            channelSettingsLauncher.launch(
                Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS)
                    .putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
                    .putExtra(Settings.EXTRA_CHANNEL_ID, FavoriteEpisodeNotifier.CHANNEL_ID),
            )
        } else {
            finishNotificationPermission()
        }
    }

    private fun enableNotifications(animeId: Int?) {
        mainViewModel.requestNotifications(animeId)
        if (NotificationManagerCompat.from(this).areNotificationsEnabled()) {
            checkNotificationChannel()
            return
        }
        if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            val preferences = getPreferences(MODE_PRIVATE)
            val asked = preferences.getBoolean("asked_notification_permission", false)
            if (!asked || shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS)) {
                preferences.edit().putBoolean("asked_notification_permission", true).apply()
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                return
            }
        }
        notificationSettingsLauncher.launch(
            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, packageName),
        )
    }
}
