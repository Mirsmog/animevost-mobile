package com.animevost.app.core.domain.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class WatchActionTest {
    private val episodes = (1..4).map { Episode("$it серия", "video-$it", "", it) }

    private fun progress(episode: Int, percent: Int) = WatchProgress(
        animeId = 10, episodeVideoId = "video-$episode", episodeName = "$episode серия",
        episodeIndex = episode - 1, positionMs = percent * 1000L, durationMs = 100_000L,
        updatedAt = episode.toLong(),
    )

    @Test
    fun resumesAtTheLastUnfinishedEpisode() {
        val last = progress(3, 84)
        assertEquals(
            WatchAction.Play(episodes[2], WatchAction.Reason.Resume),
            nextWatchAction(episodes, listOf(progress(2, 90), last)),
        )
    }

    @Test
    fun offersTheFollowingEpisodeAtTheCompletionThreshold() {
        assertEquals(WatchAction.Play(episodes[3], WatchAction.Reason.Next), nextWatchAction(episodes, listOf(progress(3, 85))))
    }

    @Test
    fun `only replay ignores the first episode's unfinished position`() {
        val unfinished = progress(1, 30)
        val resume = assertIs<WatchAction.Play>(nextWatchAction(episodes, listOf(unfinished)))
        val replay = assertIs<WatchAction.Play>(nextWatchAction(episodes, listOf(unfinished, progress(4, 100))))
        assertEquals(episodes.first(), replay.episode)
        assertTrue(replay.startFromBeginning)
        assertFalse(resume.startFromBeginning)
    }

    @Test
    fun waitsForAnAnnouncedEpisodeAfterCatchingUp() {
        val release = UpcomingEpisode(5, 1_900_000_000)
        assertEquals(WatchAction.Upcoming(release), nextWatchAction(episodes, listOf(progress(4, 99)), release))
    }

    @Test
    fun distinguishesAReplayFromAFirstVisitAndIgnoresRemovedEpisodes() {
        assertEquals(WatchAction.Play(episodes.first(), WatchAction.Reason.Replay), nextWatchAction(episodes, listOf(progress(4, 100))))
        assertEquals(WatchAction.Play(episodes.first(), WatchAction.Reason.Start), nextWatchAction(episodes, listOf(progress(6, 15))))
        assertNull(nextWatchAction(emptyList(), emptyList()))
    }
}
