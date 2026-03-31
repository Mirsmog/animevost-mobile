package com.animevost.app.core.data.repository

import com.animevost.app.core.domain.model.Schedule
import com.animevost.app.core.domain.repository.ScheduleRepository
import com.animevost.app.core.domain.util.Result
import com.animevost.app.core.network.DleEndpoints
import com.animevost.app.core.network.HtmlFetcher
import com.animevost.app.core.network.parser.ScheduleParser
import kotlinx.coroutines.CancellationException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ScheduleRepositoryImpl @Inject constructor(
    private val htmlFetcher: HtmlFetcher,
    private val scheduleParser: ScheduleParser,
) : ScheduleRepository {

    override suspend fun getSchedule(): Result<List<Schedule>> {
        return try {
            val html = htmlFetcher.fetch(DleEndpoints.BASE_URL + DleEndpoints.SCHEDULE)
            Result.Success(scheduleParser.parse(html))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.Error(e, e.message)
        }
    }
}
