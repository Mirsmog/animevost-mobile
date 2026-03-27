package com.animevost.app.core.network.parser

import com.animevost.app.core.domain.model.AnimeListResult
import javax.inject.Inject

class SearchParser @Inject constructor(
    private val animeListParser: AnimeListParser,
) {
    fun parse(html: String): AnimeListResult = animeListParser.parse(html)
}
