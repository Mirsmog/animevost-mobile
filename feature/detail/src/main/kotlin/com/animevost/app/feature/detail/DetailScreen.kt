package com.animevost.app.feature.detail

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Comment
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.RemoveRedEye
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.animevost.app.core.domain.model.AnimeDetail
import com.animevost.app.core.domain.model.Comment
import com.animevost.app.core.domain.model.Episode
import com.animevost.app.core.ui.components.AnimeCard
import com.animevost.app.core.ui.components.ErrorState
import com.animevost.app.core.ui.components.LoadingState

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

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)) {
            when {
                state.isLoading -> LoadingState()
                state.error != null -> ErrorState(
                    message = state.error!!,
                    onRetry = { viewModel.onEvent(DetailEvent.LoadAnime(animeUrl)) },
                )
                state.anime != null -> DetailContent(
                    anime = state.anime!!,
                    isFavorite = state.isFavorite,
                    userRating = state.userRating,
                    isDescriptionExpanded = state.isDescriptionExpanded,
                    comments = state.comments,
                    isLoadingComments = state.isLoadingComments,
                    commentText = state.commentText,
                    isAddingComment = state.isAddingComment,
                    hasMoreComments = state.hasMoreComments,
                    onBack = onBack,
                    onToggleFavorite = { viewModel.onEvent(DetailEvent.ToggleFavorite) },
                    onRate = { viewModel.onEvent(DetailEvent.RateAnime(it)) },
                    onToggleDescription = { viewModel.onEvent(DetailEvent.ToggleDescription) },
                    onPlayEpisode = { episode, index ->
                        onPlayEpisode(episode, state.anime!!.episodes, index)
                    },
                    onDownloadEpisode = { episode ->
                        viewModel.onEvent(DetailEvent.DownloadEpisode(episode))
                    },
                    onGenreClick = onGenreClick,
                    onRelatedClick = onRelatedClick,
                    onLoadMoreComments = { viewModel.onEvent(DetailEvent.LoadMoreComments) },
                    onCommentTextChange = { viewModel.onEvent(DetailEvent.UpdateCommentText(it)) },
                    onSubmitComment = { viewModel.onEvent(DetailEvent.SubmitComment) },
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun DetailContent(
    anime: AnimeDetail,
    isFavorite: Boolean,
    userRating: Int,
    isDescriptionExpanded: Boolean,
    comments: List<Comment>,
    isLoadingComments: Boolean,
    commentText: String,
    isAddingComment: Boolean,
    hasMoreComments: Boolean,
    onBack: () -> Unit,
    onToggleFavorite: () -> Unit,
    onRate: (Int) -> Unit,
    onToggleDescription: () -> Unit,
    onPlayEpisode: (Episode, Int) -> Unit,
    onDownloadEpisode: (Episode) -> Unit,
    onGenreClick: (String) -> Unit,
    onRelatedClick: (String) -> Unit,
    onLoadMoreComments: () -> Unit,
    onCommentTextChange: (String) -> Unit,
    onSubmitComment: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        // --- Immersive header with poster ---
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
                                Color.Black.copy(alpha = 0.85f),
                            ),
                            startY = 0f,
                            endY = Float.POSITIVE_INFINITY,
                        ),
                    ),
            )
            // Top bar
            TopAppBar(
                title = { },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Назад",
                            tint = Color.White,
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onToggleFavorite) {
                        Icon(
                            if (isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                            contentDescription = "Избранное",
                            tint = if (isFavorite) MaterialTheme.colorScheme.primary else Color.White,
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                ),
            )
            // Title over poster
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(16.dp),
            ) {
                Text(
                    text = anime.title,
                    style = MaterialTheme.typography.headlineMedium,
                    color = Color.White,
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

        // --- Info section ---
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            InfoRow(anime)

            Spacer(modifier = Modifier.height(8.dp))

            // Rating stars
            RatingBar(
                rating = anime.rating,
                userRating = userRating,
                onRate = onRate,
            )

            Spacer(modifier = Modifier.height(12.dp))

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
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        ),
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Description
            Text(
                text = "Описание",
                style = MaterialTheme.typography.titleLarge,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = anime.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = if (isDescriptionExpanded) Int.MAX_VALUE else 4,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.animateContentSize(),
            )
            if (anime.description.length > 200) {
                TextButton(onClick = onToggleDescription) {
                    Text(
                        text = if (isDescriptionExpanded) "Свернуть" else "Показать полностью",
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }

        // --- Related anime ---
        if (anime.relatedAnime.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Похожие",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
            Spacer(modifier = Modifier.height(8.dp))
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

        // --- Episodes ---
        if (anime.episodes.isNotEmpty()) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Серии",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
            Spacer(modifier = Modifier.height(8.dp))
            FlowRow(
                modifier = Modifier.padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                anime.episodes.forEachIndexed { index, episode ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        AssistChip(
                            onClick = { onPlayEpisode(episode, index) },
                            label = { Text(episode.name) },
                            leadingIcon = {
                                Icon(
                                    Icons.Filled.PlayCircle,
                                    contentDescription = null,
                                    modifier = Modifier.size(AssistChipDefaults.IconSize),
                                )
                            },
                        )
                        IconButton(
                            onClick = { onDownloadEpisode(episode) },
                            modifier = Modifier.size(32.dp),
                        ) {
                            Icon(
                                Icons.Filled.Download,
                                contentDescription = "Скачать ${episode.name}",
                                modifier = Modifier.size(20.dp),
                                tint = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                }
            }
        }

        // --- Actions row ---
        Spacer(modifier = Modifier.height(16.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Filled.RemoveRedEye,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "${anime.viewCount}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.AutoMirrored.Filled.Comment,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "${anime.commentCount}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        // --- Comments section ---
        Spacer(modifier = Modifier.height(16.dp))
        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
        Spacer(modifier = Modifier.height(16.dp))

        CommentsSection(
            comments = comments,
            isLoading = isLoadingComments,
            commentText = commentText,
            isAddingComment = isAddingComment,
            hasMore = hasMoreComments,
            onLoadMore = onLoadMoreComments,
            onTextChange = onCommentTextChange,
            onSubmit = onSubmitComment,
        )

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun CommentsSection(
    comments: List<Comment>,
    isLoading: Boolean,
    commentText: String,
    isAddingComment: Boolean,
    hasMore: Boolean,
    onLoadMore: () -> Unit,
    onTextChange: (String) -> Unit,
    onSubmit: () -> Unit,
) {
    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        Text(
            text = "Комментарии (${comments.size})",
            style = MaterialTheme.typography.titleLarge,
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Add comment input
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedTextField(
                value = commentText,
                onValueChange = onTextChange,
                modifier = Modifier.weight(1f),
                placeholder = { Text("Написать комментарий…") },
                maxLines = 3,
                enabled = !isAddingComment,
            )
            IconButton(
                onClick = onSubmit,
                enabled = commentText.isNotBlank() && !isAddingComment,
            ) {
                if (isAddingComment) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                } else {
                    Icon(
                        Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Отправить",
                        tint = if (commentText.isNotBlank()) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Comment list
        comments.forEach { comment ->
            CommentItem(comment = comment)
            Spacer(modifier = Modifier.height(8.dp))
        }

        // Loading / Load more
        if (isLoading && comments.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(modifier = Modifier.size(32.dp))
            }
        }

        if (hasMore && !isLoading) {
            TextButton(
                onClick = onLoadMore,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Показать ещё")
            }
        }

        if (isLoading && comments.isNotEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp))
            }
        }
    }
}

@Composable
private fun CommentItem(comment: Comment) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                AsyncImage(
                    model = comment.avatar,
                    contentDescription = comment.author,
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop,
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = comment.author,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                    )
                    if (comment.date.isNotBlank()) {
                        Text(
                            text = comment.date,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = comment.text,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

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
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
private fun RatingBar(
    rating: Double,
    userRating: Int,
    onRate: (Int) -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = String.format("%.1f", rating),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.primary,
        )
        Spacer(modifier = Modifier.width(8.dp))
        (1..5).forEach { star ->
            IconButton(
                onClick = { onRate(star) },
                modifier = Modifier.size(28.dp),
            ) {
                Icon(
                    imageVector = if (star <= userRating) Icons.Filled.Star else Icons.Filled.StarBorder,
                    contentDescription = "Оценка $star",
                    tint = if (star <= userRating) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    modifier = Modifier.size(24.dp),
                )
            }
        }
    }
}
