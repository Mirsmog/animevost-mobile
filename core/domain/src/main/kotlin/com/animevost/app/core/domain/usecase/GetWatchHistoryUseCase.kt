package com.animevost.app.core.domain.usecase

import com.animevost.app.core.domain.model.AnimePreview
import com.animevost.app.core.domain.repository.HistoryRepository
import javax.inject.Inject

class GetWatchHistoryUseCase @Inject constructor(
    private val repository: HistoryRepository,
) {
    suspend operator fun invoke(): List<AnimePreview> {
        return repository.getHistory()
    }
}
