package com.animevost.app.feature.player

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.animevost.app.core.domain.model.Comment
import com.animevost.app.core.ui.components.CommentHtmlRenderer
import com.animevost.app.core.ui.theme.Bg0
import com.animevost.app.core.ui.theme.Bg3
import com.animevost.app.core.ui.theme.Bg4
import com.animevost.app.core.ui.theme.OrangePrimary
import com.animevost.app.core.ui.theme.TextPrimary
import com.animevost.app.core.ui.theme.TextSecondary

@Composable
internal fun EpisodeCommentsPanel(
    episodeNumber: Int,
    comments: List<Comment>,
    isLoading: Boolean,
    hasMore: Boolean,
    error: String?,
    onClose: () -> Unit,
    onLoadMore: () -> Unit,
    onRetry: () -> Unit,
    onWriteComment: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxHeight()
            .width(360.dp)
            .pointerInput(Unit) { detectTapGestures(onTap = {}) },
        color = Bg0,
        shadowElevation = 18.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding(),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, top = 8.dp, end = 8.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Комментарии",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                    )
                    Text(
                        text = "$episodeNumber серия",
                        style = MaterialTheme.typography.bodyMedium,
                        color = OrangePrimary,
                    )
                }
                IconButton(onClick = onClose) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "Закрыть комментарии",
                        tint = TextPrimary,
                    )
                }
            }

            HorizontalDivider(color = Bg4)

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
            ) {
                when {
                    isLoading && comments.isEmpty() -> {
                        CircularProgressIndicator(
                            color = OrangePrimary,
                            modifier = Modifier
                                .align(Alignment.Center)
                            .size(36.dp),
                        )
                    }
                    comments.isEmpty() && error != null -> {
                        CommentLoadError(
                            message = error,
                            onRetry = onRetry,
                            modifier = Modifier.align(Alignment.Center),
                        )
                    }
                    comments.isEmpty() && error == null -> {
                        Column(
                            modifier = Modifier
                                .align(Alignment.Center)
                                .padding(horizontal = 28.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Text(
                                text = "Комментариев к этой серии пока нет",
                                style = MaterialTheme.typography.bodyLarge,
                                color = TextSecondary,
                            )
                            if (hasMore) {
                                Spacer(Modifier.height(8.dp))
                                TextButton(onClick = onLoadMore) {
                                    Text("Искать дальше", color = OrangePrimary)
                                }
                            }
                        }
                    }
                    else -> {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        ) {
                            items(comments, key = { it.id }) { comment ->
                                EpisodeCommentItem(comment)
                                HorizontalDivider(color = Bg4)
                            }
                            if (error != null) {
                                item {
                                    CommentLoadError(
                                        message = error,
                                        onRetry = onRetry,
                                    )
                                }
                            } else if (hasMore && !isLoading) {
                                item {
                                    TextButton(
                                        onClick = onLoadMore,
                                        modifier = Modifier.fillMaxWidth(),
                                    ) {
                                        Text("Показать ещё", color = OrangePrimary)
                                    }
                                }
                            }
                            if (isLoading) {
                                item {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 12.dp),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        CircularProgressIndicator(
                                            color = OrangePrimary,
                                            modifier = Modifier.size(26.dp),
                                            strokeWidth = 3.dp,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            HorizontalDivider(color = Bg4)
            Button(
                onClick = onWriteComment,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = OrangePrimary,
                    contentColor = Bg0,
                ),
                shape = RoundedCornerShape(12.dp),
            ) {
                Text(
                    text = "Оставить комментарий",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

@Composable
private fun EpisodeCommentItem(comment: Comment) {
    val depthPadding = (comment.depth * 10).coerceAtMost(30).dp
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .padding(start = depthPadding, top = 10.dp, bottom = 10.dp),
    ) {
        Box(
            modifier = Modifier
                .width(2.dp)
                .fillMaxHeight()
                .background(Bg4),
        )
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
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
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                if (comment.date.isNotBlank()) {
                    Text(
                        text = comment.date,
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary,
                        maxLines = 1,
                    )
                }
            }

            if (comment.quotedAuthor.isNotBlank()) {
                Spacer(Modifier.height(6.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Bg3)
                        .padding(8.dp),
                ) {
                    Text(
                        text = "Ответ для ${comment.quotedAuthor}",
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

            if (comment.text.isNotBlank()) {
                Spacer(Modifier.height(6.dp))
                CommentHtmlRenderer(
                    html = comment.text,
                    style = MaterialTheme.typography.bodyMedium.copy(color = TextPrimary),
                )
            }
        }
    }
}

@Composable
private fun CommentLoadError(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
        )
        Spacer(Modifier.height(6.dp))
        TextButton(onClick = onRetry) {
            Text("Повторить", color = OrangePrimary)
        }
    }
}
