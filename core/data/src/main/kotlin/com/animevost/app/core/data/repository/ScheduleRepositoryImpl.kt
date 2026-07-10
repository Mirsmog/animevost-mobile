package com.animevost.app.core.data.repository

import com.animevost.app.core.domain.model.Schedule
import com.animevost.app.core.domain.repository.ScheduleRepository
import com.animevost.app.core.domain.util.Result
import com.animevost.app.core.data.sdk.toDomainOrNull
import com.animevost.sdk.AnimeVostClient
import kotlinx.coroutines.CancellationException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ScheduleRepositoryImpl @Inject constructor(
    private val client: AnimeVostClient,
) : ScheduleRepository {

    override suspend fun getSchedule(): Result<List<Schedule>> {
        return try {
            Result.Success(client.getSchedule().mapNotNull { it.toDomainOrNull() })
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.Error(e, e.message)
        }
    }
}
