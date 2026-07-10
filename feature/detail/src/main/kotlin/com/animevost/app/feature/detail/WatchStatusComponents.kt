package com.animevost.app.feature.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmarks
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.BookmarkRemove
import androidx.compose.material.icons.outlined.Bookmarks
import androidx.compose.material.icons.outlined.Cancel
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.PauseCircle
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.WatchLater
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.animevost.app.core.domain.model.AnimeStatus
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
internal fun WatchStatusSquareButton(
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
                            .background(if (isSelected) OrangeMuted else Color.Transparent)
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
    AnimeStatus.WATCHING -> Icons.Outlined.Visibility
    AnimeStatus.WATCHED -> Icons.Outlined.CheckCircle
    AnimeStatus.DROPPED -> Icons.Outlined.Cancel
    AnimeStatus.PLANNED -> Icons.Outlined.WatchLater
    AnimeStatus.ON_HOLD -> Icons.Outlined.PauseCircle
}
