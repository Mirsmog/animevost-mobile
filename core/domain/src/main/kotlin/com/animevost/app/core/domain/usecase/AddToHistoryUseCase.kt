package com.animevost.app.core.domain.usecase

import com.animevost.app.core.domain.model.AnimePreview
import com.animevost.app.core.domain.model.Episode
import com.animevost.app.core.domain.repository.HistoryRepository
import javax.inject.Inject

/** Records [episode] of [anime] as watched in the user's history. */
class AddToHistoryUseCase @Inject constructor(
    private val historyRepository: HistoryRepository,
) {
    suspend operator fun invoke(anime: AnimePreview, episode: Episode) {
        historyRepository.addToHistory(anime, episode)
    }
}
