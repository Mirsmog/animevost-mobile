package com.animevost.app.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.animevost.app.core.domain.model.AnimePreview
import com.animevost.app.core.ui.theme.AccentBlue
import com.animevost.app.core.ui.theme.AccentPurple
import com.animevost.app.core.ui.theme.OrangePrimary
import java.util.Locale

// ── Grid card (2-column): poster with title + info overlay ─────
@Composable
fun AnimeCard(
    anime: AnimePreview,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(8.dp),
) {
    val episodeCount = remember(anime.episodeInfo) { extractEpisodeCount(anime.episodeInfo) }
    val hasStats = anime.rating > 0 || anime.viewCount > 0 || anime.commentCount > 0
    val context = LocalContext.current

    Box(
        modifier = modifier
            .clip(shape)
            .clickable(onClick = onClick)
            .aspectRatio(0.67f),
    ) {
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(anime.posterUrl)
                .diskCachePolicy(CachePolicy.ENABLED)
                .memoryCachePolicy(CachePolicy.ENABLED)
                .crossfade(true)
                .build(),
            contentDescription = anime.title,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
        )
        // Gradient overlay — bottom half for info
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(if (hasStats) 120.dp else 110.dp)
                .align(Alignment.BottomCenter)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.92f)),
                    ),
                ),
        )
        // Type badge + Announcement badge — top right, same row
        Row(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(6.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            if (isAnnouncement(anime.episodeInfo)) {
                AnnouncementBadge()
            }
            if (anime.type.isNotEmpty()) {
                Text(
                    text = anime.type,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier
                        .background(
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.85f),
                            shape = RoundedCornerShape(4.dp),
                        )
                        .padding(horizontal = 5.dp, vertical = 2.dp),
                )
            }
        }
        // Title + stats at bottom
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(horizontal = 8.dp, vertical = 8.dp),
        ) {
            Text(
                text = anime.title,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = Color.White,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            // Stats (rating, views) directly under title
            if (hasStats) {
                Spacer(modifier = Modifier.height(3.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (anime.rating > 0) {
                        StatBadge(
                            icon = { Icon(Icons.Filled.Star, null, modifier = Modifier.size(10.dp), tint = OrangePrimary) },
                            text = String.format(Locale.getDefault(), "%.1f", anime.rating),
                        )
                    }
                    if (anime.viewCount > 0) {
                        StatBadge(
                            icon = { Icon(Icons.Outlined.Visibility, null, modifier = Modifier.size(10.dp), tint = Color.White.copy(alpha = 0.6f)) },
                            text = formatCount(anime.viewCount),
                        )
                    }
                }
            }
            // Episode count at the bottom
            if (episodeCount.isNotEmpty()) {
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = episodeCount,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.55f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun StatBadge(
    icon: @Composable () -> Unit,
    text: String,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        icon()
        Spacer(modifier = Modifier.width(2.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = Color.White.copy(alpha = 0.75f),
        )
    }
}

private fun formatCount(count: Int): String = when {
    count >= 1_000_000 -> String.format(Locale.getDefault(), "%.1fM", count / 1_000_000.0)
    count >= 1_000 -> String.format(Locale.getDefault(), "%.1fK", count / 1_000.0)
    else -> count.toString()
}

// Extract episode info: "1-12 из 12" stays as is, strips type prefix if present
private fun extractEpisodeCount(info: String): String {
    if (info.isBlank()) return ""
    return if (info.contains("/")) info.substringAfter("/").trim() else info.trim()
}

private fun isAnnouncement(episodeInfo: String): Boolean =
    episodeInfo.contains("Анонс", ignoreCase = true)

@Composable
private fun AnnouncementBadge(modifier: Modifier = Modifier) {
    Text(
        text = "АНОНС",
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Bold,
        color = Color.White,
        modifier = modifier
            .background(
                color = AccentBlue.copy(alpha = 0.9f),
                shape = RoundedCornerShape(4.dp),
            )
            .padding(horizontal = 5.dp, vertical = 2.dp),
    )
}

// ── Horizontal card: poster + info row (search results, history) ─
@Composable
fun AnimeCardHorizontal(
    anime: AnimePreview,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    trailingContent: @Composable (() -> Unit)? = null,
) {
    val hasStats = anime.rating > 0 || anime.viewCount > 0 || anime.commentCount > 0
    val context = LocalContext.current

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(anime.posterUrl)
                .diskCachePolicy(CachePolicy.ENABLED)
                .memoryCachePolicy(CachePolicy.ENABLED)
                .crossfade(true)
                .build(),
            contentDescription = anime.title,
            modifier = Modifier
                .size(width = 56.dp, height = 80.dp)
                .clip(RoundedCornerShape(6.dp)),
            contentScale = ContentScale.Crop,
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = anime.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurface,
            )
            if (anime.episodeInfo.isNotBlank()) {
                Spacer(modifier = Modifier.height(3.dp))
                if (isAnnouncement(anime.episodeInfo)) {
                    AnnouncementBadge()
                } else {
                    Text(
                        text = anime.episodeInfo,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            if (hasStats) {
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (anime.rating > 0) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Star, null, modifier = Modifier.size(12.dp), tint = OrangePrimary)
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(
                                text = String.format(Locale.getDefault(), "%.1f", anime.rating),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    if (anime.viewCount > 0) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.Visibility, null, modifier = Modifier.size(12.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(
                                text = formatCount(anime.viewCount),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    if (anime.commentCount > 0) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.ChatBubbleOutline, null, modifier = Modifier.size(12.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(
                                text = formatCount(anime.commentCount),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
        trailingContent?.invoke()
    }
}

// ── Featured card: full-width hero for carousel ───────────────
