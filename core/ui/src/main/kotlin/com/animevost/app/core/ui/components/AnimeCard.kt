package com.animevost.app.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import coil.compose.AsyncImage
import com.animevost.app.core.domain.model.AnimePreview

// ── Grid card (2-column): poster with title + info overlay ─────
@Composable
fun AnimeCard(
    anime: AnimePreview,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Parse episodeInfo e.g. "TV / 12 из 24" or "OVA" or "12 серий"
    val animeType = remember(anime.episodeInfo) { extractAnimeType(anime.episodeInfo) }
    val episodeCount = remember(anime.episodeInfo) { extractEpisodeCount(anime.episodeInfo) }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .aspectRatio(0.67f),
    ) {
        AsyncImage(
            model = anime.posterUrl,
            contentDescription = anime.title,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
        )
        // Gradient overlay — bottom half for info
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(110.dp)
                .align(Alignment.BottomCenter)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.92f)),
                    ),
                ),
        )
        // Type badge — top right
        if (animeType.isNotEmpty()) {
            Text(
                text = animeType,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(6.dp)
                    .background(
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.85f),
                        shape = RoundedCornerShape(4.dp),
                    )
                    .padding(horizontal = 5.dp, vertical = 2.dp),
            )
        }
        // Title + episode count at bottom
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
            if (episodeCount.isNotEmpty()) {
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = episodeCount,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.65f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

// Extract type token: TV, OVA, ONA, Movie, etc.
private fun extractAnimeType(info: String): String {
    if (info.isBlank()) return ""
    val types = listOf("TV", "OVA", "ONA", "Movie", "Special", "Фильм", "Спэшл")
    for (type in types) {
        if (info.contains(type, ignoreCase = true)) return type.uppercase()
    }
    return ""
}

// Extract episode info: "12 из 24" or "12 серий" etc.
private fun extractEpisodeCount(info: String): String {
    if (info.isBlank()) return ""
    // Remove type prefix: "TV / 12 из 24" → "12 из 24"
    val stripped = info.replace(Regex("^(TV|OVA|ONA|Movie|Special|Фильм|Спэшл)\\s*/\\s*", RegexOption.IGNORE_CASE), "").trim()
    return if (stripped.isNotEmpty() && stripped != info.trim()) stripped else {
        // If no type prefix, return full info as fallback
        info
    }
}

// ── Horizontal card: poster + info row (search results, history) ─
@Composable
fun AnimeCardHorizontal(
    anime: AnimePreview,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    trailingContent: @Composable (() -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AsyncImage(
            model = anime.posterUrl,
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
                Text(
                    text = anime.episodeInfo,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        trailingContent?.invoke()
    }
}

// ── Featured card: full-width hero for carousel ───────────────
@Composable
fun AnimeCardFeatured(
    anime: AnimePreview,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(260.dp)
            .clip(RoundedCornerShape(0.dp))
            .clickable(onClick = onClick),
    ) {
        AsyncImage(
            model = anime.posterUrl,
            contentDescription = anime.title,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
        )
        // Dark gradient from bottom
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colorStops = arrayOf(
                            0.0f to Color.Transparent,
                            0.45f to Color.Black.copy(alpha = 0.15f),
                            1.0f to Color.Black.copy(alpha = 0.88f),
                        ),
                    ),
                ),
        )
        // Info overlay at bottom-left
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(horizontal = 16.dp, vertical = 16.dp),
        ) {
            Text(
                text = anime.title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (anime.episodeInfo.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = anime.episodeInfo,
                    style = MaterialTheme.typography.labelLarge,
                    color = Color.White.copy(alpha = 0.75f),
                    maxLines = 1,
                )
            }
        }
    }
}
