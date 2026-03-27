package com.animevost.app.core.domain.usecase

import com.animevost.app.core.domain.model.AnimePreview
import com.animevost.app.core.domain.model.Episode
import com.animevost.app.core.domain.repository.HistoryRepository
import javax.inject.Inject

class AddToHistoryUseCase @Inject constructor(
    private val historyRepository: HistoryRepository,
) {
    suspend operator fun invoke(anime: AnimePreview, episode: Episode) {
        historyRepository.addToHistory(anime, episode)
    }
}
