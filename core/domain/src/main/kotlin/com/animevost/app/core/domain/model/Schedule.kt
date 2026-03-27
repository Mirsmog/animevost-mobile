package com.animevost.app.core.domain.model

data class Schedule(
    val dayOfWeek: String,
    val items: List<ScheduleItem>,
)

data class ScheduleItem(
    val title: String,
    val time: String,
    val url: String,
)
