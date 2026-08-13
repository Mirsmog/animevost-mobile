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
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Bookmarks
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.outlined.Bookmarks
import androidx.compose.material.icons.filled.RemoveRedEye
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.outlined.EmojiEmotions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
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
import java.util.Locale

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
    val state by viewModel.uiState.collectAsStateWithLifecycle()
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
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            when {
                state.isLoading -> LoadingState()
                state.error != null -> ErrorState(
                    message = state.error!!,
                    onRetry = { viewModel.onEvent(DetailEvent.LoadAnime(animeUrl)) },
                )
                state.anime != null -> DetailContent(
                    anime = state.anime!!,
                    isFavorite = state.isFavorite,
                    areFavoriteNotificationsEnabled = state.areFavoriteNotificationsEnabled,
                    isFavoriteNotificationMuted = state.isFavoriteNotificationMuted,
                    isLoggedIn = state.isLoggedIn,
                    userRating = state.userRating,
                    isRatingSubmitting = state.isRatingSubmitting,
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
                    replyTarget = state.replyTarget,
                    isPreparingReply = state.isPreparingReply,
                    hasMoreComments = state.hasMoreComments,
                    deletingCommentId = state.deletingCommentId,
                    onBack = onBack,
                    onToggleFavorite = { viewModel.onEvent(DetailEvent.ToggleFavorite) },
                    onToggleFavoriteNotification = {
                        viewModel.onEvent(DetailEvent.ToggleFavoriteNotification)
                    },
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
                    onCancelReply = { viewModel.onEvent(DetailEvent.CancelReply) },
                    onReportComment = { comment -> viewModel.onEvent(DetailEvent.ReportComment(comment)) },
                    onDeleteComment = { comment -> viewModel.onEvent(DetailEvent.RequestDeleteComment(comment)) },
                )
            }
        }
    }

    state.reportTarget?.let { comment ->
        ReportCommentDialog(
            comment = comment,
            value = state.reportTextValue,
            isSubmitting = state.isReportingComment,
            onValueChange = { viewModel.onEvent(DetailEvent.UpdateReportTextValue(it)) },
            onSubmit = { viewModel.onEvent(DetailEvent.SubmitReport) },
            onDismiss = { viewModel.onEvent(DetailEvent.DismissReport) },
        )
    }

    state.deleteTarget?.let { comment ->
        DeleteCommentDialog(
            comment = comment,
            isDeleting = state.deletingCommentId == comment.id,
            onConfirm = { viewModel.onEvent(DetailEvent.ConfirmDeleteComment) },
            onDismiss = { viewModel.onEvent(DetailEvent.DismissDeleteComment) },
        )
    }
}

@Composable
private fun ReportCommentDialog(
    comment: Comment,
    value: TextFieldValue,
    isSubmitting: Boolean,
    onValueChange: (TextFieldValue) -> Unit,
    onSubmit: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = { if (!isSubmitting) onDismiss() },
        containerColor = Bg2,
        title = {
            Text(
                text = "Жалоба на ${comment.author}",
                color = TextPrimary,
                style = MaterialTheme.typography.titleMedium,
            )
        },
        text = {
            Column {
                Text(
                    text = "Коротко опиши нарушение. Жалоба уйдет администрации AnimeVost.",
                    color = TextSecondary,
                    style = MaterialTheme.typography.bodySmall,
                )
                Spacer(Modifier.height(10.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 92.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Bg3)
                        .padding(12.dp),
                ) {
                    if (value.text.isBlank()) {
                        Text(
                            text = "Причина жалобы",
                            color = TextSecondary,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                    BasicTextField(
                        value = value,
                        onValueChange = onValueChange,
                        enabled = !isSubmitting,
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = MaterialTheme.typography.bodyMedium.copy(color = TextPrimary),
                        cursorBrush = SolidColor(OrangePrimary),
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onSubmit,
                enabled = value.text.isNotBlank() && !isSubmitting,
            ) {
                Text(if (isSubmitting) "Отправка..." else "Отправить", color = OrangePrimary)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isSubmitting) {
                Text("Отмена", color = TextSecondary)
            }
        },
    )
}

@Composable
private fun DeleteCommentDialog(
    comment: Comment,
    isDeleting: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = { if (!isDeleting) onDismiss() },
        containerColor = Bg2,
        title = {
            Text(
                text = "Удалить комментарий?",
                color = TextPrimary,
                style = MaterialTheme.typography.titleMedium,
            )
        },
        text = {
            Text(
                text = comment.text.stripCommentPreview().ifBlank { "Это действие нельзя отменить." },
                color = TextSecondary,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm, enabled = !isDeleting) {
                Text(if (isDeleting) "Удаление..." else "Удалить", color = ErrorRed)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isDeleting) {
                Text("Отмена", color = TextSecondary)
            }
        },
    )
}

// ── Main content ──────────────────────────────────────────────────────────────

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DetailContent(
    anime: AnimeDetail,
    isFavorite: Boolean,
    areFavoriteNotificationsEnabled: Boolean,
    isFavoriteNotificationMuted: Boolean,
    isLoggedIn: Boolean,
    userRating: Int,
    isRatingSubmitting: Boolean,
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
    replyTarget: Comment?,
    isPreparingReply: Boolean,
    hasMoreComments: Boolean,
    deletingCommentId: Int?,
    onBack: () -> Unit,
    onToggleFavorite: () -> Unit,
    onToggleFavoriteNotification: () -> Unit,
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
    onCancelReply: () -> Unit,
    onReportComment: (Comment) -> Unit,
    onDeleteComment: (Comment) -> Unit,
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
            showFavoriteNotificationAction = anime.releaseStatus.supportsEpisodeNotifications,
            isFavoriteNotificationEnabled =
                areFavoriteNotificationsEnabled && !isFavoriteNotificationMuted,
            onBack = onBack,
            onToggleFavorite = onToggleFavorite,
            onToggleFavoriteNotification = onToggleFavoriteNotification,
        )

        // ── Info section ─────────────────────────────────────────────
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            InfoRow(anime)
            Spacer(Modifier.height(8.dp))
            RatingBar(
                rating = anime.rating,
                userRating = userRating,
                isLoggedIn = isLoggedIn,
                isSubmitting = isRatingSubmitting,
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
            totalCommentCount = anime.commentCount,
            isLoading = isLoadingComments,
            isLoggedIn = isLoggedIn,
            commentTextValue = commentTextValue,
            isAddingComment = isAddingComment,
            replyTarget = replyTarget,
            isPreparingReply = isPreparingReply,
            hasMore = hasMoreComments,
            deletingCommentId = deletingCommentId,
            onLoadMore = onLoadMoreComments,
            onTextValueChange = onCommentTextValueChange,
            onSubmit = onSubmitComment,
            onReply = onReplyToComment,
            onCancelReply = onCancelReply,
            onReport = onReportComment,
            onDelete = onDeleteComment,
        )

        Spacer(Modifier.height(24.dp).navigationBarsPadding())
    }
}

// ── Poster Header ─────────────────────────────────────────────────────────────

@Composable
private fun PosterHeader(
    anime: AnimeDetail,
    isFavorite: Boolean,
    showFavoriteNotificationAction: Boolean,
    isFavoriteNotificationEnabled: Boolean,
    onBack: () -> Unit,
    onToggleFavorite: () -> Unit,
    onToggleFavoriteNotification: () -> Unit,
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

        Row(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .statusBarsPadding()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (isFavorite && showFavoriteNotificationAction) {
                HeaderActionButton(
                    onClick = onToggleFavoriteNotification,
                    contentDescription = if (isFavoriteNotificationEnabled) {
                        "Отключить уведомления"
                    } else {
                        "Включить уведомления"
                    },
                ) {
                    Icon(
                        imageVector = if (isFavoriteNotificationEnabled) {
                            Icons.Filled.Notifications
                        } else {
                            Icons.Filled.NotificationsOff
                        },
                        contentDescription = null,
                        tint = if (isFavoriteNotificationEnabled) OrangePrimary else Color.White,
                    )
                }
            }
            HeaderActionButton(
                onClick = onToggleFavorite,
                contentDescription = "Избранное",
            ) {
                Icon(
                    imageVector = if (isFavorite) {
                        Icons.Filled.Favorite
                    } else {
                        Icons.Filled.FavoriteBorder
                    },
                    contentDescription = null,
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

@Composable
private fun HeaderActionButton(
    onClick: () -> Unit,
    contentDescription: String,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(Bg0.copy(alpha = 0.45f)),
        contentAlignment = Alignment.Center,
    ) {
        IconButton(
            onClick = onClick,
            modifier = Modifier.size(40.dp),
        ) {
            Box(modifier = Modifier.semantics { this.contentDescription = contentDescription }) {
                content()
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
    totalCommentCount: Int,
    isLoading: Boolean,
    isLoggedIn: Boolean,
    commentTextValue: TextFieldValue,
    isAddingComment: Boolean,
    replyTarget: Comment?,
    isPreparingReply: Boolean,
    hasMore: Boolean,
    deletingCommentId: Int?,
    onLoadMore: () -> Unit,
    onTextValueChange: (TextFieldValue) -> Unit,
    onSubmit: () -> Unit,
    onReply: (Comment) -> Unit,
    onCancelReply: () -> Unit,
    onReport: (Comment) -> Unit,
    onDelete: (Comment) -> Unit,
) {
    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = "Комментарии",
                style = MaterialTheme.typography.titleLarge,
                color = TextPrimary,
            )
            Spacer(Modifier.width(8.dp))
            val counterText = if (totalCommentCount > comments.size) {
                "${comments.size} из $totalCommentCount"
            } else {
                comments.size.toString()
            }
            Text(
                text = counterText,
                style = MaterialTheme.typography.labelLarge,
                color = TextSecondary,
                modifier = Modifier.padding(bottom = 2.dp),
            )
        }
        Spacer(Modifier.height(12.dp))

        if (isLoggedIn && replyTarget == null) {
            CommentEditor(
                commentTextValue = commentTextValue,
                isAddingComment = isAddingComment,
                title = null,
                placeholder = "Написать комментарий...",
                onTextValueChange = onTextValueChange,
                onSubmit = onSubmit,
                onCancel = null,
            )
        } else if (!isLoggedIn) {
            Text(
                text = "Войдите в аккаунт, чтобы оставить комментарий",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
            )
        }

        Spacer(Modifier.height(12.dp))

        comments.forEachIndexed { index, comment ->
            CommentItem(
                comment = comment,
                isLoggedIn = isLoggedIn,
                isDeleting = deletingCommentId == comment.id,
                onReply = onReply,
                onReport = onReport,
                onDelete = onDelete,
            )
            if (replyTarget?.id == comment.id && isLoggedIn) {
                Spacer(Modifier.height(4.dp))
                CommentEditor(
                    commentTextValue = commentTextValue,
                    isAddingComment = isAddingComment,
                    isPreparing = isPreparingReply,
                    title = "Ответ для ${comment.author}",
                    placeholder = if (isPreparingReply) "Готовим цитату..." else "Написать ответ...",
                    onTextValueChange = onTextValueChange,
                    onSubmit = onSubmit,
                    onCancel = onCancelReply,
                    modifier = Modifier.padding(start = ((comment.depth + 1) * 14).coerceAtMost(56).dp),
                )
                Spacer(Modifier.height(8.dp))
            }
            if (index < comments.lastIndex) {
                HorizontalDivider(
                    modifier = Modifier.padding(start = ((comment.depth + 1) * 14).coerceAtMost(56).dp),
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
    modifier: Modifier = Modifier,
    isPreparing: Boolean = false,
    title: String?,
    placeholder: String,
    onTextValueChange: (TextFieldValue) -> Unit,
    onSubmit: () -> Unit,
    onCancel: (() -> Unit)?,
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
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Bg2),
    ) {
        val topPadding = if (title == null && onCancel == null) 16.dp else 10.dp
        Column(
            modifier = Modifier.padding(
                start = 10.dp,
                top = topPadding,
                end = 10.dp,
                bottom = 10.dp,
            ),
        ) {
            if (title != null || onCancel != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (title != null) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f),
                        )
                    } else {
                        Spacer(Modifier.weight(1f))
                    }
                    if (onCancel != null) {
                        TextButton(
                            onClick = onCancel,
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            enabled = !isAddingComment,
                        ) {
                            Text(
                                text = "Отмена",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondary,
                            )
                        }
                    }
                }
                Spacer(Modifier.height(6.dp))
            }

            // Text input or preview
            if (showPreview) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 64.dp)
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
                        .heightIn(min = 64.dp)
                        .padding(bottom = 8.dp),
                ) {
                    if (commentTextValue.text.isEmpty()) {
                        Text(
                            text = placeholder,
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
                        enabled = !isAddingComment && !isPreparing,
                        maxLines = 8,
                        visualTransformation = EmojiTagVisualTransformation,
                    )
                }
            }

            // Emoji grid shown above toolbar
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
                                .clickable { insertAtCursor(":$id:") },
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
                // Formatting buttons group, active only when text is selected
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

                TextButton(
                    onClick = { showPreview = !showPreview },
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                    enabled = !isPreparing,
                ) {
                    Text(
                        text = if (showPreview) "Редактор" else "Превью",
                        style = MaterialTheme.typography.labelSmall,
                        color = OrangePrimary,
                    )
                }

                IconButton(
                    onClick = { showEmojiPicker = !showEmojiPicker },
                    enabled = !isPreparing,
                    modifier = Modifier.size(36.dp),
                ) {
                    Icon(
                        Icons.Outlined.EmojiEmotions,
                        contentDescription = "Эмодзи",
                        tint = if (showEmojiPicker) OrangePrimary else TextSecondary,
                        modifier = Modifier.size(22.dp),
                    )
                }

                IconButton(
                    onClick = onSubmit,
                    enabled = commentTextValue.text.isNotBlank() && !isAddingComment && !isPreparing,
                    modifier = Modifier.size(36.dp),
                ) {
                    if (isAddingComment || isPreparing) {
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

// ── Comment Item ──────────────────────────────────────────────────────────────

@Composable
private fun CommentItem(
    comment: Comment,
    isLoggedIn: Boolean,
    isDeleting: Boolean,
    onReply: (Comment) -> Unit,
    onReport: (Comment) -> Unit,
    onDelete: (Comment) -> Unit,
) {
    val depthPadding = (comment.depth * 14).coerceAtMost(56).dp
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .padding(start = depthPadding, top = 8.dp, bottom = 8.dp),
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
                comment.ordinal?.let { ordinal ->
                    Text(
                        text = "#$ordinal",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary.copy(alpha = 0.7f),
                    )
                }
            }
            comment.authorCommentCount?.let { count ->
                Text(
                    text = "$count комм.",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary.copy(alpha = 0.7f),
                    modifier = Modifier.padding(start = 36.dp, top = 1.dp),
                )
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

            if (isLoggedIn && (comment.canReply || comment.canReport || comment.canDelete)) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (comment.canReply) {
                        TextButton(
                            onClick = { onReply(comment) },
                            contentPadding = PaddingValues(horizontal = 0.dp, vertical = 0.dp),
                        ) {
                            Text(
                                text = "Ответить",
                                style = MaterialTheme.typography.labelSmall,
                                color = OrangePrimary,
                            )
                        }
                    }
                    if (comment.canReport) {
                        TextButton(
                            onClick = { onReport(comment) },
                            contentPadding = PaddingValues(horizontal = 0.dp, vertical = 0.dp),
                        ) {
                            Text(
                                text = "Жалоба",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondary,
                            )
                        }
                    }
                    if (comment.canDelete) {
                        TextButton(
                            onClick = { onDelete(comment) },
                            enabled = !isDeleting,
                            contentPadding = PaddingValues(horizontal = 0.dp, vertical = 0.dp),
                        ) {
                            Text(
                                text = if (isDeleting) "Удаление..." else "Удалить",
                                style = MaterialTheme.typography.labelSmall,
                                color = ErrorRed,
                            )
                        }
                    }
                }
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
    isSubmitting: Boolean,
    onRate: (Int) -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = String.format(Locale.getDefault(), "%.1f", rating),
            style = MaterialTheme.typography.titleLarge,
            color = OrangePrimary,
        )
        Spacer(Modifier.width(8.dp))
        if (isLoggedIn) {
            val hasUserRating = userRating > 0
            val displayedRating = if (hasUserRating) {
                userRating
            } else {
                rating.toInt().coerceIn(0, 5)
            }
            (1..5).forEach { star ->
                val isFilled = star <= displayedRating
                IconButton(
                    onClick = { onRate(star) },
                    enabled = !isSubmitting,
                    modifier = Modifier.size(28.dp),
                ) {
                    Icon(
                        imageVector = if (isFilled) Icons.Filled.Star else Icons.Filled.StarBorder,
                        contentDescription = "Оценка $star",
                        tint = when {
                            isSubmitting -> TextSecondary.copy(alpha = 0.35f)
                            hasUserRating && isFilled -> OrangePrimary
                            isFilled -> TextSecondary.copy(alpha = 0.65f)
                            else -> TextSecondary
                        },
                        modifier = Modifier.size(24.dp),
                    )
                }
            }
            if (isSubmitting) {
                Spacer(Modifier.width(6.dp))
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    color = OrangePrimary,
                    strokeWidth = 2.dp,
                )
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
    count >= 1_000_000 -> String.format(Locale.getDefault(), "%.1fM", count / 1_000_000.0)
    count >= 1_000 -> String.format(Locale.getDefault(), "%.1fK", count / 1_000.0)
    else -> count.toString()
}

private fun String.stripCommentPreview(): String =
    replace(Regex("""<!--smile:\d+-->.*?<!--/smile-->""", RegexOption.DOT_MATCHES_ALL), " ")
        .replace(EMOJI_INPUT_TOKEN_REGEX) { match ->
            if (match.animeVostEmojiIdOrNull() != null) " " else match.value
        }
        .replace(Regex("""<[^>]+>"""), " ")
        .replace("&nbsp;", " ")
        .replace("&quot;", "\"")
        .replace("&#39;", "'")
        .replace("&amp;", "&")
        .replace(Regex("""\s+"""), " ")
        .trim()

private val EMOJI_INPUT_TOKEN_REGEX = Regex("""<!--smile:(\d+)-->|(?<!\d):(\d{1,3}):""")

private val EmojiTagVisualTransformation = VisualTransformation { text ->
    val original = text.text
    val sb = StringBuilder()
    val originalToTransformed = IntArray(original.length + 1)
    val transformedToOriginal = mutableListOf<Int>()
    var lastEnd = 0

    for (match in EMOJI_INPUT_TOKEN_REGEX.findAll(original)) {
        val emojiId = match.animeVostEmojiIdOrNull() ?: continue
        val before = original.substring(lastEnd, match.range.first)
        for (i in before.indices) {
            originalToTransformed[lastEnd + i] = sb.length + i
            transformedToOriginal.add(lastEnd + i)
        }
        sb.append(before)
        val replacement = "[:$emojiId:]"
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

private fun MatchResult.animeVostEmojiIdOrNull(): String? {
    val raw = groupValues.getOrNull(1)?.takeIf { it.isNotBlank() }
        ?: groupValues.getOrNull(2)?.takeIf { it.isNotBlank() }
    val id = raw?.toIntOrNull() ?: return null
    return id.takeIf { it in 1..100 || it == 102 }?.toString()
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
