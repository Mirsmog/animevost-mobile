package com.animevost.app.core.domain.model

sealed interface WatchAction {
    data class Play(val episode: Episode, val reason: Reason) : WatchAction {
        val startFromBeginning: Boolean get() = reason == Reason.Replay
    }
    data class Upcoming(val episode: UpcomingEpisode) : WatchAction
    enum class Reason { Start, Resume, Next, Replay }
}

fun nextWatchAction(
    episodes: List<Episode>,
    progress: List<WatchProgress>,
    upcomingEpisode: UpcomingEpisode? = null,
): WatchAction? {
    val first = episodes.firstOrNull() ?: return upcomingEpisode?.let(WatchAction::Upcoming)
    val byId = episodes.associateBy { it.videoId }
    val last = progress.filter { it.episodeVideoId in byId }.maxByOrNull { it.updatedAt }
        ?: return WatchAction.Play(first, WatchAction.Reason.Start)
    val current = byId.getValue(last.episodeVideoId)
    if (!last.isCompleted) return WatchAction.Play(current, WatchAction.Reason.Resume)

    val watched = progress.filter { it.isCompleted }.mapTo(mutableSetOf()) { it.episodeVideoId }
    val next = episodes.drop(episodes.indexOf(current) + 1).firstOrNull { it.videoId !in watched }
    if (next != null) return WatchAction.Play(next, WatchAction.Reason.Next)
    val upcoming = upcomingEpisode?.takeUnless { release -> episodes.any { it.number == release.number } }
    return upcoming?.let(WatchAction::Upcoming) ?: WatchAction.Play(first, WatchAction.Reason.Replay)
}
