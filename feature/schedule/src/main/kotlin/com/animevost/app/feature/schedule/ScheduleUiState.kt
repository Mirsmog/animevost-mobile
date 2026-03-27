package com.animevost.app.feature.schedule

import com.animevost.app.core.domain.model.ScheduleItem
import java.time.DayOfWeek

data class ScheduleUiState(
    val selectedDay: DayTab = DayTab.fromToday(),
    val scheduleItems: List<ScheduleItem> = emptyList(),
    val allSchedule: Map<String, List<ScheduleItem>> = emptyMap(),
    val isLoading: Boolean = false,
    val error: String? = null,
)

sealed interface ScheduleEvent {
    data class SelectDay(val day: DayTab) : ScheduleEvent
    data object Refresh : ScheduleEvent
    data object ClearError : ScheduleEvent
}

enum class DayTab(
    val shortName: String,
    val fullName: String,
    val dayOfWeek: DayOfWeek,
) {
    MONDAY("Пн", "Понедельник", DayOfWeek.MONDAY),
    TUESDAY("Вт", "Вторник", DayOfWeek.TUESDAY),
    WEDNESDAY("Ср", "Среда", DayOfWeek.WEDNESDAY),
    THURSDAY("Чт", "Четверг", DayOfWeek.THURSDAY),
    FRIDAY("Пт", "Пятница", DayOfWeek.FRIDAY),
    SATURDAY("Сб", "Суббота", DayOfWeek.SATURDAY),
    SUNDAY("Вс", "Воскресенье", DayOfWeek.SUNDAY);

    companion object {
        fun fromToday(): DayTab {
            val today = java.time.LocalDate.now().dayOfWeek
            return entries.find { it.dayOfWeek == today } ?: MONDAY
        }
    }
}
