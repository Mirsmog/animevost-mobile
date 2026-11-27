package com.animevost.app.feature.player

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.changedToUp
import kotlin.math.abs

internal suspend fun PointerInputScope.detectPlayerMotion(
    onPinch: (zoomToFill: Boolean) -> Unit,
    onScrubStart: () -> Unit,
    onScrub: (Float) -> Unit,
    onScrubEnd: (commit: Boolean) -> Unit,
) {
    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
        var horizontal: Boolean? = null
        var initialSpan: Float? = null
        var scaled = false
        var scrubbing = false
        var released = false
        try {
            do {
                val event = awaitPointerEvent(PointerEventPass.Initial)
                val fingers = event.changes.filter { it.pressed }
                if (fingers.size >= 2) {
                    if (scrubbing) {
                        onScrubEnd(false)
                        scrubbing = false
                    }
                    val span = (fingers[0].position - fingers[1].position).getDistance()
                    val start = initialSpan ?: span.also { initialSpan = it }
                    if (!scaled && abs(span - start) > viewConfiguration.touchSlop * 2) {
                        onPinch(span > start)
                        scaled = true
                    }
                } else if (initialSpan == null && fingers.isNotEmpty()) {
                    val offset = fingers.first().position - down.position
                    if (horizontal == null && offset.getDistance() > viewConfiguration.touchSlop) {
                        horizontal = abs(offset.x) > abs(offset.y)
                        if (horizontal) {
                            scrubbing = true
                            onScrubStart()
                        }
                    }
                    if (scrubbing) onScrub(offset.x / size.width.coerceAtLeast(1))
                }
                if (fingers.isEmpty()) released = event.changes.any { it.changedToUp() }
                if (initialSpan != null || horizontal != null) {
                    event.changes.forEach { it.consume() }
                }
            } while (fingers.isNotEmpty())
        } finally {
            if (scrubbing) onScrubEnd(released)
        }
    }
}

// Observe before the controls and video surface handle the event. Neither child loses its gesture.
internal suspend fun PointerInputScope.observePlayerTouches(onHeld: (Boolean) -> Unit) {
    awaitEachGesture {
        awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
        onHeld(true)
        try {
            do {
                val event = awaitPointerEvent(PointerEventPass.Initial)
            } while (event.changes.any { it.pressed })
        } finally {
            onHeld(false)
        }
    }
}
