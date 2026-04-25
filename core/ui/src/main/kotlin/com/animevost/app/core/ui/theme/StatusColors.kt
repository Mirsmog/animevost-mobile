package com.animevost.app.core.ui.theme

import androidx.compose.ui.graphics.Color
import com.animevost.app.core.domain.model.AnimeStatus

/**
 * Returns the brand accent color used to represent [status] on the library screen
 * (status tiles, status pills on cards). All colors are tuned for the dark theme.
 */
fun AnimeStatus.accentColor(): Color = when (this) {
    AnimeStatus.WATCHING -> StatusWatching
    AnimeStatus.PLANNED -> StatusPlanned
    AnimeStatus.WATCHED -> StatusCompleted
    AnimeStatus.DROPPED -> StatusDropped
    AnimeStatus.ON_HOLD -> StatusOnHold
}
