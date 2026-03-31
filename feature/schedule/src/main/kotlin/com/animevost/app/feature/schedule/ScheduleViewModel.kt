package com.animevost.app.feature.schedule

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.animevost.app.core.domain.usecase.GetScheduleUseCase
import com.animevost.app.core.domain.util.onError
import com.animevost.app.core.domain.util.onSuccess
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ScheduleViewModel @Inject constructor(
    private val getScheduleUseCase: GetScheduleUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ScheduleUiState())
    val uiState: StateFlow<ScheduleUiState> = _uiState.asStateFlow()

    init {
        loadSchedule()
    }

    fun onEvent(event: ScheduleEvent) {
        when (event) {
            is ScheduleEvent.SelectDay -> selectDay(event.day)
            ScheduleEvent.Refresh -> loadSchedule()
            ScheduleEvent.ClearError -> _uiState.update { it.copy(error = null) }
        }
    }

    private fun selectDay(day: DayTab) {
        _uiState.update {
            it.copy(
                selectedDay = day,
                scheduleItems = it.allSchedule[day.fullName].orEmpty(),
            )
        }
    }

    private fun loadSchedule() {
        _uiState.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            getScheduleUseCase()
                .onSuccess { schedules ->
                    val scheduleMap = schedules.associate { it.dayOfWeek to it.items }
                    val selectedDay = _uiState.value.selectedDay
                    _uiState.update {
                        it.copy(
                            allSchedule = scheduleMap,
                            scheduleItems = scheduleMap[selectedDay.fullName].orEmpty(),
                            isLoading = false,
                        )
                    }
                }
                .onError { _, msg ->
                    _uiState.update {
                        it.copy(isLoading = false, error = msg ?: "Ошибка загрузки расписания")
                    }
                }
        }
    }
}
