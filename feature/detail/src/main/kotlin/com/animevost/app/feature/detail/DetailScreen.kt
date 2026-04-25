package com.animevost.app.feature.detail

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Comment
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Bookmarks
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.outlined.Bookmarks
import androidx.compose.material.icons.filled.RemoveRedEye
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.outlined.EmojiEmotions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material.icons.outlined.BookmarkRemove
import androidx.compose.material.icons.outlined.Cancel
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.PauseCircle
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.WatchLater
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.animevost.app.core.domain.model.AnimeDetail
import com.animevost.app.core.domain.model.AnimeStatus
import com.animevost.app.core.domain.model.Comment
import com.animevost.app.core.domain.model.Episode
import com.animevost.app.core.domain.model.VideoSource
import com.animevost.app.core.ui.components.AnimeCard
import com.animevost.app.core.ui.components.ErrorState
import com.animevost.app.core.ui.components.LoadingState
import com.animevost.app.core.ui.theme.Bg0
import com.animevost.app.core.ui.theme.Bg1
import com.animevost.app.core.ui.theme.Bg2
import com.animevost.app.core.ui.theme.Bg3
import com.animevost.app.core.ui.theme.Bg4
import com.animevost.app.core.ui.theme.ErrorRed
import com.animevost.app.core.ui.theme.OrangeMuted
import com.animevost.app.core.ui.theme.OrangePrimary
import com.animevost.app.core.ui.theme.TextPrimary
import com.animevost.app.core.ui.theme.TextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(
    animeUrl: String,
    onBack: () -> Unit,
    onPlayEpisode: (Episode, List<Episode>, Int) -> Unit,
    onGenreClick: (String) -> Unit,
    onRelatedClick: (String) -> Unit,
    viewModel: DetailViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(animeUrl) {
        viewModel.onEvent(DetailEvent.LoadAnime(animeUrl))
    }

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is DetailEffect.ShowSnackbar -> snackbarHostState.showSnackbar(effect.message)
            }
        }
    }

    // Download quality bottom sheet
    val downloadEpisodePending = state.downloadEpisodePending
    if (downloadEpisodePending != null) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { viewModel.onEvent(DetailEvent.HideDownloadSheet) },
            sheetState = sheetState,
            containerColor = Bg2,
        ) {
            DownloadQualitySheet(
                episode = downloadEpisodePending,
                sources = state.downloadSources,
                isLoading = state.isLoadingDownloadSources,
                onDownload = { source -> viewModel.onEvent(DetailEvent.DownloadWithQuality(source)) },
            )
        }
    }

    Scaffold(
        containerColor = Bg1,
        contentWindowInsets = WindowInsets(0),
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { _ ->
        Box(modifier = Modifier.fillMaxSize()) {
            when {
                state.isLoading -> LoadingState()
                state.error != null -> ErrorState(
                    message = state.error!!,
                    onRetry = { viewModel.onEvent(DetailEvent.LoadAnime(animeUrl)) },
                )
                state.anime != null -> DetailContent(
                    anime = state.anime!!,
                    isFavorite = state.isFavorite,
                    isLoggedIn = state.isLoggedIn,
                    userRating = state.userRating,
                    isDescriptionExpanded = state.isDescriptionExpanded,
                    episodeRangeStart = state.episodeRangeStart,
                    watchedEpisodeIds = state.watchedEpisodeIds,
                    continueEpisode = state.continueEpisode,
                    continuePositionMs = state.continuePositionMs,
                    watchStatus = state.watchStatus,
                    watchStatusEnabled = state.watchStatusEnabled,
                    comments = state.comments,
                    isLoadingComments = state.isLoadingComments,
                    commentTextValue = state.commentTextValue,
                    isAddingComment = state.isAddingComment,
                    hasMoreComments = state.hasMoreComments,
                    onBack = onBack,
                    onToggleFavorite = { viewModel.onEvent(DetailEvent.ToggleFavorite) },
                    onRate = { viewModel.onEvent(DetailEvent.RateAnime(it)) },
                    onToggleDescription = { viewModel.onEvent(DetailEvent.ToggleDescription) },
                    onSetWatchStatus = { viewModel.onEvent(DetailEvent.SetWatchStatus(it)) },
                    onPlayEpisode = { episode, index ->
                        onPlayEpisode(episode, state.anime!!.episodes, index)
                    },                    onShowDownloadSheet = { episode ->
                        viewModel.onEvent(DetailEvent.ShowDownloadSheet(episode))
                    },
                    onSelectEpisodeRange = { start ->
                        viewModel.onEvent(DetailEvent.SelectEpisodeRange(start))
                    },
                    onGenreClick = onGenreClick,
                    onRelatedClick = onRelatedClick,
                    onLoadMoreComments = { viewModel.onEvent(DetailEvent.LoadMoreComments) },
                    onCommentTextValueChange = { viewModel.onEvent(DetailEvent.UpdateCommentTextValue(it)) },
                    onSubmitComment = { viewModel.onEvent(DetailEvent.SubmitComment) },
                    onReplyToComment = { comment -> viewModel.onEvent(DetailEvent.ReplyToComment(comment)) },
                )
            }
        }
    }
}

// ── Main content ──────────────────────────────────────────────────────────────

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DetailContent(
    anime: AnimeDetail,
    isFavorite: Boolean,
    isLoggedIn: Boolean,
    userRating: Int,
    isDescriptionExpanded: Boolean,
    episodeRangeStart: Int,
    watchedEpisodeIds: Set<String>,
    continueEpisode: Episode?,
    continuePositionMs: Long,
    watchStatus: AnimeStatus?,
    watchStatusEnabled: Boolean,
    comments: List<Comment>,
    isLoadingComments: Boolean,
    commentTextValue: TextFieldValue,
    isAddingComment: Boolean,
    hasMoreComments: Boolean,
    onBack: () -> Unit,
    onToggleFavorite: () -> Unit,
    onRate: (Int) -> Unit,
    onToggleDescription: () -> Unit,
    onSetWatchStatus: (AnimeStatus?) -> Unit,
    onPlayEpisode: (Episode, Int) -> Unit,
    onShowDownloadSheet: (Episode) -> Unit,
    onSelectEpisodeRange: (Int) -> Unit,
    onGenreClick: (String) -> Unit,
    onRelatedClick: (String) -> Unit,
    onLoadMoreComments: () -> Unit,
    onCommentTextValueChange: (TextFieldValue) -> Unit,
    onSubmitComment: () -> Unit,
    onReplyToComment: (Comment) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Bg1)
            .verticalScroll(rememberScrollState()),
    ) {
        // ── Immersive poster header ──────────────────────────────────
        PosterHeader(
            anime = anime,
            isFavorite = isFavorite,
            onBack = onBack,
            onToggleFavorite = onToggleFavorite,
        )

        // ── Info section ─────────────────────────────────────────────
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            InfoRow(anime)
            Spacer(Modifier.height(8.dp))
            RatingBar(
                rating = anime.rating,
                userRating = userRating,
                isLoggedIn = isLoggedIn,
                onRate = onRate,
            )
            Spacer(Modifier.height(16.dp))

            // CTA row: Watch / Continue + Watch-list button
            ActionButtonsRow(
                episodes = anime.episodes,
                continueEpisode = continueEpisode,
                watchStatus = watchStatus,
                watchStatusEnabled = watchStatusEnabled,
                onPlayEpisode = onPlayEpisode,
                onSetWatchStatus = onSetWatchStatus,
            )
            Spacer(Modifier.height(12.dp))

            // Genre chips
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                anime.genres.forEach { genre ->
                    FilterChip(
                        selected = false,
                        onClick = { onGenreClick(genre.url) },
                        label = {
                            Text(
                                text = genre.name,
                                style = MaterialTheme.typography.labelMedium,
                            )
                        },
                        shape = RoundedCornerShape(16.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = Bg3,
                            labelColor = TextSecondary,
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = false,
                            borderColor = Color.Transparent,
                        ),
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // Description
            Text(
                text = "Описание",
                style = MaterialTheme.typography.titleLarge,
                color = TextPrimary,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = anime.description,
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                maxLines = if (isDescriptionExpanded) Int.MAX_VALUE else 4,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.animateContentSize(),
            )
            if (anime.description.length > 200) {
                TextButton(
                    onClick = onToggleDescription,
                    contentPadding = PaddingValues(horizontal = 0.dp, vertical = 4.dp),
                ) {
                    Text(
                        text = if (isDescriptionExpanded) "Свернуть" else "Показать полностью",
                        color = OrangePrimary,
                    )
                }
            }
        }

        // ── Related anime ─────────────────────────────────────────────
        if (anime.relatedAnime.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Похожие",
                style = MaterialTheme.typography.titleLarge,
                color = TextPrimary,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
            Spacer(Modifier.height(8.dp))
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(anime.relatedAnime, key = { it.id }) { related ->
                    AnimeCard(
                        anime = related,
                        onClick = { onRelatedClick(related.url) },
                        modifier = Modifier.width(130.dp),
                    )
                }
            }
        }

        // ── Series parts ──────────────────────────────────────────────
        if (anime.relatedSeries.isNotEmpty()) {
            Spacer(Modifier.height(16.dp))
            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = Bg4)
            Spacer(Modifier.height(16.dp))
            Text(
                text = "Это аниме состоит из:",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
            Spacer(Modifier.height(8.dp))
            Column(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                anime.relatedSeries.forEachIndexed { index, series ->
                    Row(verticalAlignment = Alignment.Top) {
                        Text(
                            text = "${index + 1}. ",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary,
                        )
                        Column {
                            Text(
                                text = series.title,
                                style = MaterialTheme.typography.bodyMedium,
                                color = OrangePrimary,
                                modifier = Modifier.clickable { onRelatedClick(series.url) },
                            )
                            if (series.description.isNotEmpty()) {
                                Text(
                                    text = series.description,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary,
                                )
                            }
                        }
                    }
                }
            }
        }

        // ── Episodes ──────────────────────────────────────────────────
        if (anime.episodes.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = Bg4)
            Spacer(Modifier.height(12.dp))
            EpisodesSection(
                episodes = anime.episodes,
                episodeRangeStart = episodeRangeStart,
                watchedEpisodeIds = watchedEpisodeIds,
                onPlayEpisode = onPlayEpisode,
                onShowDownloadSheet = onShowDownloadSheet,
                onSelectEpisodeRange = onSelectEpisodeRange,
            )
        }

        // ── Stats row ─────────────────────────────────────────────────
        Spacer(Modifier.height(12.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Filled.RemoveRedEye,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = TextSecondary,
                )
                Spacer(Modifier.width(5.dp))
                Text(
                    text = formatStatCount(anime.viewCount),
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                )
                Spacer(Modifier.width(3.dp))
                Text(
                    text = "просмотров",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary.copy(alpha = 0.6f),
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.AutoMirrored.Filled.Comment,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = TextSecondary,
                )
                Spacer(Modifier.width(5.dp))
                Text(
                    text = formatStatCount(anime.commentCount),
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                )
                Spacer(Modifier.width(3.dp))
                Text(
                    text = "комментариев",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary.copy(alpha = 0.6f),
                )
            }
        }

        // ── Comments ──────────────────────────────────────────────────
        Spacer(Modifier.height(16.dp))
        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = Bg4)
        Spacer(Modifier.height(16.dp))
        CommentsSection(
            comments = comments,
            isLoading = isLoadingComments,
            isLoggedIn = isLoggedIn,
            commentTextValue = commentTextValue,
            isAddingComment = isAddingComment,
            hasMore = hasMoreComments,
            onLoadMore = onLoadMoreComments,
            onTextValueChange = onCommentTextValueChange,
            onSubmit = onSubmitComment,
            onReply = onReplyToComment,
        )

        Spacer(Modifier.height(24.dp).navigationBarsPadding())
    }
}

// ── Poster Header ─────────────────────────────────────────────────────────────

@Composable
private fun PosterHeader(
    anime: AnimeDetail,
    isFavorite: Boolean,
    onBack: () -> Unit,
    onToggleFavorite: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(0.75f),
    ) {
        AsyncImage(
            model = anime.posterUrl,
            contentDescription = anime.title,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
        )
        // Gradient overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.3f),
                            Color.Black.copy(alpha = 0.9f),
                        ),
                    ),
                ),
        )

        // Back button — top left with scrim backdrop
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .statusBarsPadding()
                .padding(12.dp)
                .size(40.dp)
                .clip(CircleShape)
                .background(Bg0.copy(alpha = 0.45f)),
            contentAlignment = Alignment.Center,
        ) {
            IconButton(onClick = onBack, modifier = Modifier.size(40.dp)) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Назад",
                    tint = Color.White,
                )
            }
        }

        // Favorite button — top right with scrim backdrop
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .statusBarsPadding()
                .padding(12.dp)
                .size(40.dp)
                .clip(CircleShape)
                .background(Bg0.copy(alpha = 0.45f)),
            contentAlignment = Alignment.Center,
        ) {
            IconButton(onClick = onToggleFavorite, modifier = Modifier.size(40.dp)) {
                Icon(
                    imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                    contentDescription = "Избранное",
                    tint = if (isFavorite) OrangePrimary else Color.White,
                )
            }
        }

        // Title at bottom
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(16.dp),
        ) {
            Text(
                text = anime.title,
                style = MaterialTheme.typography.headlineMedium,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
            if (anime.titleOriginal.isNotBlank()) {
                Text(
                    text = anime.titleOriginal,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.7f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

// ── Episodes Section ──────────────────────────────────────────────────────────

@Composable
private fun EpisodesSection(
    episodes: List<Episode>,
    episodeRangeStart: Int,
    watchedEpisodeIds: Set<String>,
    onPlayEpisode: (Episode, Int) -> Unit,
    onShowDownloadSheet: (Episode) -> Unit,
    onSelectEpisodeRange: (Int) -> Unit,
) {
    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Серии",
                style = MaterialTheme.typography.titleLarge,
                color = TextPrimary,
            )
            Text(
                text = "${episodes.size} эп.",
                style = MaterialTheme.typography.labelLarge,
                color = TextSecondary,
            )
        }

        // Range chips — only when more than 50 episodes
        if (episodes.size > 50) {
            Spacer(Modifier.height(8.dp))
            val ranges = remember(episodes.size) { (0 until episodes.size step 50).toList() }
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 4.dp),
            ) {
                items(ranges) { start ->
                    val end = minOf(start + 50, episodes.size)
                    FilterChip(
                        selected = start == episodeRangeStart,
                        onClick = { onSelectEpisodeRange(start) },
                        label = {
                            Text(
                                text = "${start + 1}–$end",
                                style = MaterialTheme.typography.labelMedium,
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = OrangePrimary,
                            selectedLabelColor = Color.Black,
                            containerColor = Bg3,
                            labelColor = TextSecondary,
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = start == episodeRangeStart,
                            borderColor = Color.Transparent,
                            selectedBorderColor = Color.Transparent,
                        ),
                    )
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        val displayEpisodes = if (episodes.size > 50) {
            val end = minOf(episodeRangeStart + 50, episodes.size)
            episodes.subList(episodeRangeStart, end)
        } else {
            episodes
        }

        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            displayEpisodes.forEachIndexed { localIndex, episode ->
                val globalIndex = if (episodes.size > 50) episodeRangeStart + localIndex else localIndex
                val isWatched = episode.videoId in watchedEpisodeIds
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onPlayEpisode(episode, globalIndex) }
                        .padding(vertical = 10.dp, horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(if (isWatched) OrangePrimary.copy(alpha = 0.2f) else Bg3),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (isWatched) {
                            Icon(
                                Icons.Filled.CheckCircle,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                tint = OrangePrimary,
                            )
                        } else {
                            Text(
                                text = "${globalIndex + 1}",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = TextSecondary,
                            )
                        }
                    }
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = episode.name,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (isWatched) TextSecondary else TextPrimary,
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    IconButton(
                        onClick = { onShowDownloadSheet(episode) },
                        modifier = Modifier.size(36.dp),
                    ) {
                        Icon(
                            Icons.Filled.Download,
                            contentDescription = "Скачать ${episode.name}",
                            modifier = Modifier.size(18.dp),
                            tint = TextSecondary,
                        )
                    }
                }
                if (localIndex < displayEpisodes.lastIndex) {
                    HorizontalDivider(
                        modifier = Modifier.padding(start = 56.dp),
                        color = Bg4,
                    )
                }
            }
        }
    }
}

// ── Download Quality Sheet ────────────────────────────────────────────────────

@Composable
private fun DownloadQualitySheet(
    episode: Episode,
    sources: List<VideoSource>,
    isLoading: Boolean,
    onDownload: (VideoSource) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .padding(bottom = 32.dp),
    ) {
        Text(
            text = "Выберите качество",
            style = MaterialTheme.typography.titleMedium,
            color = TextPrimary,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = episode.name,
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary,
            modifier = Modifier.padding(top = 2.dp, bottom = 16.dp),
        )

        when {
            isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(color = OrangePrimary)
                }
            }
            sources.isEmpty() -> {
                Text(
                    text = "Нет доступных источников",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                    modifier = Modifier.padding(vertical = 16.dp),
                )
            }
            else -> {
                sources.forEach { source ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(Bg3)
                            .clickable { onDownload(source) }
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            Icons.Filled.Download,
                            contentDescription = null,
                            tint = OrangePrimary,
                            modifier = Modifier.size(20.dp),
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(
                            text = source.quality,
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextPrimary,
                            modifier = Modifier.weight(1f),
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                }
            }
        }
    }
}

// ── Comments Section ──────────────────────────────────────────────────────────

@Composable
private fun CommentsSection(
    comments: List<Comment>,
    isLoading: Boolean,
    isLoggedIn: Boolean,
    commentTextValue: TextFieldValue,
    isAddingComment: Boolean,
    hasMore: Boolean,
    onLoadMore: () -> Unit,
    onTextValueChange: (TextFieldValue) -> Unit,
    onSubmit: () -> Unit,
    onReply: (Comment) -> Unit,
) {
    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        Text(
            text = "Комментарии (${comments.size})",
            style = MaterialTheme.typography.titleLarge,
            color = TextPrimary,
        )
        Spacer(Modifier.height(12.dp))

        if (isLoggedIn) {
            CommentEditor(
                commentTextValue = commentTextValue,
                isAddingComment = isAddingComment,
                onTextValueChange = onTextValueChange,
                onSubmit = onSubmit,
            )
        } else {
            Text(
                text = "Войдите в аккаунт, чтобы оставить комментарий",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
            )
        }

        Spacer(Modifier.height(12.dp))

        // Flat comment list
        comments.forEachIndexed { index, comment ->
            CommentItem(comment = comment, onReply = onReply)
            if (index < comments.lastIndex) {
                HorizontalDivider(
                    modifier = Modifier.padding(start = 40.dp),
                    color = Bg4,
                )
            }
        }

        if (isLoading && comments.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(modifier = Modifier.size(32.dp), color = OrangePrimary)
            }
        }

        if (hasMore && !isLoading) {
            TextButton(onClick = onLoadMore, modifier = Modifier.fillMaxWidth()) {
                Text(text = "Показать ещё", color = OrangePrimary)
            }
        }

        if (isLoading && comments.isNotEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), color = OrangePrimary)
            }
        }
    }
}

// ── Comment Editor Card ───────────────────────────────────────────────────────

@Composable
private fun CommentEditor(
    commentTextValue: TextFieldValue,
    isAddingComment: Boolean,
    onTextValueChange: (TextFieldValue) -> Unit,
    onSubmit: () -> Unit,
) {
    var showEmojiPicker by remember { mutableStateOf(false) }
    var showPreview by remember { mutableStateOf(false) }

    fun applyFormat(openTag: String, closeTag: String) {
        val sel = commentTextValue.selection
        val newText: String
        val newCursor: Int
        if (sel.length > 0) {
            newText = commentTextValue.text.substring(0, sel.start) + openTag +
                    commentTextValue.text.substring(sel.start, sel.end) + closeTag +
                    commentTextValue.text.substring(sel.end)
            newCursor = sel.end + openTag.length + closeTag.length
        } else {
            newText = commentTextValue.text.substring(0, sel.start) + openTag + closeTag +
                    commentTextValue.text.substring(sel.start)
            newCursor = sel.start + openTag.length
        }
        onTextValueChange(commentTextValue.copy(text = newText, selection = TextRange(newCursor)))
    }

    fun insertAtCursor(insertion: String) {
        val sel = commentTextValue.selection
        val newText = commentTextValue.text.substring(0, sel.start) + insertion +
                commentTextValue.text.substring(sel.start)
        onTextValueChange(
            commentTextValue.copy(
                text = newText,
                selection = TextRange(sel.start + insertion.length),
            ),
        )
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Bg2),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Preview toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                TextButton(
                    onClick = { showPreview = !showPreview },
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                ) {
                    Text(
                        text = if (showPreview) "Редактор" else "Превью",
                        style = MaterialTheme.typography.labelSmall,
                        color = OrangePrimary,
                    )
                }
            }

            // Text input or preview
            if (showPreview) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 80.dp)
                        .padding(bottom = 8.dp),
                ) {
                    if (commentTextValue.text.isBlank()) {
                        Text(
                            text = "Нет текста для предпросмотра",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                        )
                    } else {
                        CommentHtmlRenderer(
                            html = commentTextValue.text,
                            style = MaterialTheme.typography.bodyMedium.copy(color = TextPrimary),
                        )
                    }
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 80.dp)
                        .padding(bottom = 8.dp),
                ) {
                    if (commentTextValue.text.isEmpty()) {
                        Text(
                            text = "Написать комментарий…",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary,
                        )
                    }
                    BasicTextField(
                        value = commentTextValue,
                        onValueChange = onTextValueChange,
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = MaterialTheme.typography.bodyMedium.copy(color = TextPrimary),
                        cursorBrush = SolidColor(OrangePrimary),
                        enabled = !isAddingComment,
                        maxLines = 8,
                        visualTransformation = EmojiTagVisualTransformation,
                    )
                }
            }

            // Emoji grid — shown above toolbar
            AnimatedVisibility(
                visible = showEmojiPicker,
                enter = expandVertically(),
                exit = shrinkVertically(),
            ) {
                val emojiIds = remember { (1..100).toList() + listOf(102) }
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(40.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                        .padding(bottom = 8.dp),
                    contentPadding = PaddingValues(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    gridItems(emojiIds) { id ->
                        AsyncImage(
                            model = "https://animevost.org/engine/data/emoticons/$id.gif",
                            contentDescription = "emoji $id",
                            modifier = Modifier
                                .size(36.dp)
                                .clickable { insertAtCursor("<!--smile:$id-->") },
                        )
                    }
                }
            }

            HorizontalDivider(color = Bg4)
            Spacer(Modifier.height(8.dp))

            // Toolbar row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Formatting buttons group — active only when text is selected
                val hasSelection = commentTextValue.selection.length > 0
                Row(
                    modifier = Modifier
                        .background(Bg3, RoundedCornerShape(8.dp))
                        .padding(horizontal = 4.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    FormatTextButton(
                        label = "B",
                        fontWeight = FontWeight.Bold,
                        enabled = hasSelection,
                    ) { applyFormat("[b]", "[/b]") }
                    FormatTextButton(
                        label = "S",
                        textDecoration = TextDecoration.LineThrough,
                        enabled = hasSelection,
                    ) { applyFormat("[s]", "[/s]") }

                    // Spoiler button: eye icon + text
                    Box(
                        modifier = Modifier
                            .height(32.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .then(if (hasSelection) Modifier.clickable { applyFormat("[spoiler]", "[/spoiler]") } else Modifier)
                            .padding(horizontal = 6.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp),
                        ) {
                            Icon(
                                Icons.Filled.RemoveRedEye,
                                contentDescription = null,
                                tint = if (hasSelection) TextSecondary else TextSecondary.copy(alpha = 0.35f),
                                modifier = Modifier.size(14.dp),
                            )
                            Text(
                                text = "Спойлер",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (hasSelection) TextSecondary else TextSecondary.copy(alpha = 0.35f),
                            )
                        }
                    }
                }

                Spacer(Modifier.weight(1f))

                // Emoji picker toggle
                IconButton(
                    onClick = { showEmojiPicker = !showEmojiPicker },
                    modifier = Modifier.size(36.dp),
                ) {
                    Icon(
                        Icons.Outlined.EmojiEmotions,
                        contentDescription = "Эмодзи",
                        tint = if (showEmojiPicker) OrangePrimary else TextSecondary,
                        modifier = Modifier.size(22.dp),
                    )
                }

                // Send button
                IconButton(
                    onClick = onSubmit,
                    enabled = commentTextValue.text.isNotBlank() && !isAddingComment,
                    modifier = Modifier.size(36.dp),
                ) {
                    if (isAddingComment) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = OrangePrimary)
                    } else {
                        Icon(
                            Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Отправить",
                            tint = if (commentTextValue.text.isNotBlank()) OrangePrimary else TextSecondary,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FormatTextButton(
    label: String,
    fontWeight: FontWeight? = null,
    textDecoration: TextDecoration? = null,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(32.dp)
            .clip(RoundedCornerShape(6.dp))
            .then(if (enabled) Modifier.clickable(onClick = onClick) else Modifier),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = fontWeight ?: FontWeight.Normal,
                textDecoration = textDecoration ?: TextDecoration.None,
                color = if (enabled) TextPrimary else TextPrimary.copy(alpha = 0.3f),
            ),
        )
    }
}

// ── Comment Item — flat Reddit-style design ───────────────────────────────────

@Composable
private fun CommentItem(comment: Comment, onReply: (Comment) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .padding(vertical = 8.dp),
    ) {
        // Vertical thread line
        Box(
            modifier = Modifier
                .width(2.dp)
                .fillMaxHeight()
                .background(Bg4),
        )
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            // Header: avatar + author + date
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                AsyncImage(
                    model = comment.avatar,
                    contentDescription = comment.author,
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop,
                )
                Text(
                    text = comment.author,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                )
                if (comment.date.isNotBlank()) {
                    Text(
                        text = comment.date,
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary,
                    )
                }
            }
            Spacer(Modifier.height(6.dp))

            // Quote block
            if (comment.quotedAuthor.isNotBlank()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 6.dp)
                        .height(IntrinsicSize.Min)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Bg3),
                ) {
                    Box(
                        modifier = Modifier
                            .width(3.dp)
                            .fillMaxHeight()
                            .background(OrangePrimary),
                    )
                    Column(modifier = Modifier.padding(8.dp)) {
                        Text(
                            text = "↩ ${comment.quotedAuthor}",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = OrangePrimary,
                        )
                        if (comment.quotedText.isNotBlank()) {
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = comment.quotedText,
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }

            // Comment text
            if (comment.text.isNotBlank()) {
                CommentHtmlRenderer(
                    html = comment.text,
                    style = MaterialTheme.typography.bodyMedium.copy(color = TextPrimary),
                )
            }

            // Compact reply button
            TextButton(
                onClick = { onReply(comment) },
                contentPadding = PaddingValues(horizontal = 0.dp, vertical = 2.dp),
            ) {
                Text(
                    text = "Ответить",
                    style = MaterialTheme.typography.labelSmall,
                    color = OrangePrimary,
                )
            }
        }
    }
}

// ── Info helpers ──────────────────────────────────────────────────────────────

@Composable
private fun InfoRow(anime: AnimeDetail) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        InfoItem(label = "Год", value = anime.year)
        InfoItem(label = "Тип", value = anime.type.displayName)
        InfoItem(label = "Эпизоды", value = anime.episodeCount)
        if (anime.director.isNotBlank()) {
            InfoItem(label = "Режиссёр", value = anime.director)
        }
    }
}

@Composable
private fun InfoItem(label: String, value: String) {
    Row {
        Text(
            text = "$label: ",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = TextPrimary,
        )
    }
}

@Composable
private fun RatingBar(
    rating: Double,
    userRating: Int,
    isLoggedIn: Boolean,
    onRate: (Int) -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = String.format("%.1f", rating),
            style = MaterialTheme.typography.titleLarge,
            color = OrangePrimary,
        )
        Spacer(Modifier.width(8.dp))
        if (isLoggedIn) {
            (1..5).forEach { star ->
                IconButton(
                    onClick = { onRate(star) },
                    modifier = Modifier.size(28.dp),
                ) {
                    Icon(
                        imageVector = if (star <= userRating) Icons.Filled.Star else Icons.Filled.StarBorder,
                        contentDescription = "Оценка $star",
                        tint = if (star <= userRating) OrangePrimary else TextSecondary,
                        modifier = Modifier.size(24.dp),
                    )
                }
            }
        } else {
            (1..5).forEach { star ->
                Icon(
                    imageVector = if (star <= rating.toInt()) Icons.Filled.Star else Icons.Filled.StarBorder,
                    contentDescription = null,
                    tint = TextSecondary.copy(alpha = 0.5f),
                    modifier = Modifier
                        .size(24.dp)
                        .padding(2.dp),
                )
            }
        }
    }
}

private fun formatStatCount(count: Int): String = when {
    count >= 1_000_000 -> String.format("%.1fM", count / 1_000_000.0)
    count >= 1_000 -> String.format("%.1fK", count / 1_000.0)
    else -> count.toString()
}

/**
 * Replaces <!--smile:N--> tags with [:N:] in the visual display
 * so the text field shows compact emoji tokens instead of raw HTML comments.
 */
private val EmojiTagVisualTransformation = VisualTransformation { text ->
    val original = text.text
    val regex = Regex("""<!--smile:(\d+)-->""")
    val sb = StringBuilder()
    val originalToTransformed = IntArray(original.length + 1)
    val transformedToOriginal = mutableListOf<Int>()
    var lastEnd = 0

    for (match in regex.findAll(original)) {
        val before = original.substring(lastEnd, match.range.first)
        for (i in before.indices) {
            originalToTransformed[lastEnd + i] = sb.length + i
            transformedToOriginal.add(lastEnd + i)
        }
        sb.append(before)
        val replacement = "[:${match.groupValues[1]}:]"
        val startOriginal = match.range.first
        for (i in match.value.indices) {
            originalToTransformed[startOriginal + i] = sb.length
        }
        repeat(replacement.length) { transformedToOriginal.add(startOriginal) }
        sb.append(replacement)
        lastEnd = match.range.last + 1
    }
    val tail = original.substring(lastEnd)
    for (i in tail.indices) {
        originalToTransformed[lastEnd + i] = sb.length + i
        transformedToOriginal.add(lastEnd + i)
    }
    sb.append(tail)
    originalToTransformed[original.length] = sb.length
    transformedToOriginal.add(original.length)

    val offsetMapping = object : OffsetMapping {
        override fun originalToTransformed(offset: Int): Int =
            originalToTransformed.getOrElse(offset) { sb.length }
        override fun transformedToOriginal(offset: Int): Int =
            transformedToOriginal.getOrElse(offset) { original.length }
    }
    TransformedText(AnnotatedString(sb.toString()), offsetMapping)
}

// ── CTA: Watch + Watch-list buttons row ───────────────────────────────────────

@Composable
private fun ActionButtonsRow(
    episodes: List<Episode>,
    continueEpisode: Episode?,
    watchStatus: AnimeStatus?,
    watchStatusEnabled: Boolean,
    onPlayEpisode: (Episode, Int) -> Unit,
    onSetWatchStatus: (AnimeStatus?) -> Unit,
) {
    val hasEpisodes = episodes.isNotEmpty()
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (hasEpisodes) {
            val isContinue = continueEpisode != null
            Button(
                onClick = {
                    if (isContinue) {
                        val idx = episodes.indexOf(continueEpisode)
                        onPlayEpisode(continueEpisode!!, if (idx >= 0) idx else 0)
                    } else {
                        onPlayEpisode(episodes.first(), 0)
                    }
                },
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = OrangePrimary,
                    contentColor = Color.Black,
                ),
            ) {
                Icon(
                    Icons.Filled.PlayCircle,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = if (isContinue) "Продолжить • ${continueEpisode!!.name}" else "Смотреть",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        if (watchStatusEnabled) {
            WatchStatusSquareButton(
                currentStatus = watchStatus,
                onStatusSelected = onSetWatchStatus,
                modifier = if (hasEpisodes)
                    Modifier.size(52.dp)
                else
                    Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                showLabel = !hasEpisodes,
            )
        }
    }
}

// ── Watch-list square / full-width button ─────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WatchStatusSquareButton(
    currentStatus: AnimeStatus?,
    onStatusSelected: (AnimeStatus?) -> Unit,
    modifier: Modifier = Modifier,
    showLabel: Boolean = false,
) {
    var showSheet by remember { mutableStateOf(false) }
    val isActive = currentStatus != null
    val clickLabel = if (isActive) "Изменить статус просмотра" else "Добавить в список"

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (isActive) OrangeMuted else Bg3)
            .clickable(onClickLabel = clickLabel) { showSheet = true },
        contentAlignment = Alignment.Center,
    ) {
        if (showLabel) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(horizontal = 16.dp),
            ) {
                Icon(
                    imageVector = if (isActive) Icons.Filled.Bookmarks else Icons.Outlined.Bookmarks,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = if (isActive) OrangePrimary else TextSecondary,
                )
                Text(
                    text = currentStatus?.label ?: "В список",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isActive) OrangePrimary else TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        } else {
            Icon(
                imageVector = if (isActive) Icons.Filled.Bookmarks else Icons.Outlined.Bookmarks,
                contentDescription = clickLabel,
                modifier = Modifier.size(22.dp),
                tint = if (isActive) OrangePrimary else TextSecondary,
            )
        }
    }

    if (showSheet) {
        ModalBottomSheet(
            onDismissRequest = { showSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = Bg2,
            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
            dragHandle = {
                Box(
                    modifier = Modifier
                        .padding(vertical = 10.dp)
                        .width(36.dp)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(TextSecondary.copy(alpha = 0.3f)),
                )
            },
        ) {
            Column(modifier = Modifier.padding(bottom = 32.dp)) {
                Text(
                    text = "Статус просмотра",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                )
                Spacer(modifier = Modifier.height(4.dp))
                AnimeStatus.entries.forEach { status ->
                    val isSelected = status == currentStatus
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onStatusSelected(if (isSelected) null else status)
                                showSheet = false
                            }
                            .background(if (isSelected) OrangeMuted else androidx.compose.ui.graphics.Color.Transparent)
                            .padding(horizontal = 20.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = watchStatusIcon(status),
                            contentDescription = null,
                            modifier = Modifier.size(22.dp),
                            tint = if (isSelected) OrangePrimary else TextSecondary,
                        )
                        Spacer(modifier = Modifier.width(14.dp))
                        Text(
                            text = status.label,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (isSelected) OrangePrimary else TextPrimary,
                            modifier = Modifier.weight(1f),
                        )
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Filled.CheckCircle,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                tint = OrangePrimary,
                            )
                        }
                    }
                }
                if (currentStatus != null) {
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
                        color = Bg4,
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onStatusSelected(null)
                                showSheet = false
                            }
                            .padding(horizontal = 20.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.BookmarkRemove,
                            contentDescription = null,
                            modifier = Modifier.size(22.dp),
                            tint = ErrorRed,
                        )
                        Spacer(modifier = Modifier.width(14.dp))
                        Text(
                            text = "Убрать из списка",
                            style = MaterialTheme.typography.bodyLarge,
                            color = ErrorRed,
                        )
                    }
                }
            }
        }
    }
}

private fun watchStatusIcon(status: AnimeStatus) = when (status) {
    AnimeStatus.WATCHING  -> Icons.Outlined.Visibility
    AnimeStatus.WATCHED   -> Icons.Outlined.CheckCircle
    AnimeStatus.DROPPED   -> Icons.Outlined.Cancel
    AnimeStatus.PLANNED   -> Icons.Outlined.WatchLater
    AnimeStatus.ON_HOLD   -> Icons.Outlined.PauseCircle
}
