package com.animevost.app

import android.Manifest
import android.content.pm.PackageManager
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.core.content.ContextCompat
import com.animevost.app.core.data.notification.AnimeNotificationIntent
import com.animevost.app.core.ui.theme.AnimeVostTheme
import com.animevost.app.navigation.AppNavGraph
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.MutableStateFlow
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.animevost.app.core.domain.repository.AuthRepository
import com.animevost.app.core.domain.repository.FavoriteRepository
import com.animevost.app.core.domain.repository.UserListRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
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

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { }

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
        requestNotificationPermissionIfNeeded()
        handleNotificationIntent(intent)
        enableEdgeToEdge()
        if (BuildConfig.DEBUG) {
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        setContent {
            val animeUrl by notificationAnimeUrl.collectAsStateWithLifecycle()
            AnimeVostTheme {
                AppNavGraph(
                    notificationAnimeUrl = animeUrl,
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

    private fun requestNotificationPermissionIfNeeded() {
        if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}
