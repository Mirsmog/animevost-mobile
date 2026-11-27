package com.animevost.app.core.ui.components

import com.animevost.app.core.domain.model.WatchAction
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

fun WatchAction.label(): String = when (this) {
    is WatchAction.Play -> when (reason) {
        WatchAction.Reason.Start -> "Смотреть"
        WatchAction.Reason.Resume -> "Продолжить · " + episode.name
        WatchAction.Reason.Next -> "Дальше · " + episode.name
        WatchAction.Reason.Replay -> "Посмотреть снова"
    }
    is WatchAction.Upcoming -> {
        val date = Instant.ofEpochSecond(episode.scheduledAtEpochSeconds).atZone(ZoneId.systemDefault())
        "Серия " + episode.number + " · " + date.format(DateTimeFormatter.ofPattern("d MMMM", Locale.forLanguageTag("ru")))
    }
}
