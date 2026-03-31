package com.animevost.app.core.domain.repository

import com.animevost.app.core.domain.model.Schedule
import com.animevost.app.core.domain.util.Result

/** Provides the weekly broadcast schedule. */
interface ScheduleRepository {
    /** Returns the weekly [Schedule] list, or [Result.Error] on failure. */
    suspend fun getSchedule(): Result<List<Schedule>>
}
