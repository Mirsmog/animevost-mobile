package com.animevost.app.core.domain.repository

import com.animevost.app.core.domain.model.Schedule

interface ScheduleRepository {
    suspend fun getSchedule(): List<Schedule>
}
