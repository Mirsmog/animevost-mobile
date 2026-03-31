package com.animevost.app.core.domain.usecase

import com.animevost.app.core.domain.model.Schedule
import com.animevost.app.core.domain.repository.ScheduleRepository
import com.animevost.app.core.domain.util.Result
import javax.inject.Inject

/** Retrieves the weekly broadcast schedule. */
class GetScheduleUseCase @Inject constructor(
    private val repository: ScheduleRepository,
) {
    suspend operator fun invoke(): Result<List<Schedule>> = repository.getSchedule()
}
