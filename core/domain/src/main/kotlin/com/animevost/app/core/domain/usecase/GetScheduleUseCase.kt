package com.animevost.app.core.domain.usecase

import com.animevost.app.core.domain.model.Schedule
import com.animevost.app.core.domain.repository.ScheduleRepository
import javax.inject.Inject

class GetScheduleUseCase @Inject constructor(
    private val repository: ScheduleRepository,
) {
    suspend operator fun invoke(): List<Schedule> {
        return repository.getSchedule()
    }
}
