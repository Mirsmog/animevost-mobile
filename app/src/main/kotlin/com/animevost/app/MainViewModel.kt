package com.animevost.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.animevost.app.core.domain.model.AppBuildInfo
import com.animevost.app.core.domain.model.UpdateInfo
import com.animevost.app.core.domain.repository.UpdateRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.File
import javax.inject.Inject

sealed interface UpdateState {
    data object Idle : UpdateState
    data class Available(val info: UpdateInfo) : UpdateState
    data class Downloading(val progress: Int) : UpdateState
    data class ReadyToInstall(val file: File) : UpdateState
}

@HiltViewModel
class MainViewModel @Inject constructor(
    private val updateRepository: UpdateRepository,
    private val appBuildInfo: AppBuildInfo,
) : ViewModel() {

    private val _updateState = MutableStateFlow<UpdateState>(UpdateState.Idle)
    val updateState: StateFlow<UpdateState> = _updateState

    init {
        if (appBuildInfo.inAppUpdatesEnabled) checkForUpdates()
    }

    private fun checkForUpdates() {
        viewModelScope.launch {
            try {
                val info = updateRepository.checkForUpdate(appBuildInfo.versionName)
                if (info != null) {
                    _updateState.value = UpdateState.Available(info)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Timber.w(e, "Update check error")
            }
        }
    }

    fun startDownload(info: UpdateInfo) {
        viewModelScope.launch {
            try {
                _updateState.value = UpdateState.Downloading(0)
                val file = updateRepository.downloadUpdate(info.downloadUrl) { progress ->
                    _updateState.value = UpdateState.Downloading(progress)
                }
                _updateState.value = UpdateState.ReadyToInstall(file)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Timber.e(e, "Download failed")
                _updateState.value = UpdateState.Available(info)
            }
        }
    }

    fun dismissUpdate() {
        _updateState.value = UpdateState.Idle
    }
}
