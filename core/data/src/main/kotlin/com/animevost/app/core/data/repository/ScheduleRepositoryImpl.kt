package com.animevost.app.core.data.repository

import com.animevost.app.core.domain.model.Schedule
import com.animevost.app.core.domain.repository.ScheduleRepository
import com.animevost.app.core.network.DleEndpoints
import com.animevost.app.core.network.HtmlFetcher
import com.animevost.app.core.network.parser.ScheduleParser
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ScheduleRepositoryImpl @Inject constructor(
    private val htmlFetcher: HtmlFetcher,
    private val scheduleParser: ScheduleParser,
) : ScheduleRepository {

    override suspend fun getSchedule(): List<Schedule> {
        val html = htmlFetcher.fetch(DleEndpoints.BASE_URL + DleEndpoints.SCHEDULE)
        return scheduleParser.parse(html)
    }
}
