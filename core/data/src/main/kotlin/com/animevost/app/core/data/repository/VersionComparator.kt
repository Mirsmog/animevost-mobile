package com.animevost.app.core.data.repository

internal fun isNewerVersion(remote: String, current: String): Boolean {
    val remoteParts = remote.toVersionParts()
    val currentParts = current.toVersionParts()
    if (remoteParts.isEmpty() || currentParts.isEmpty()) return false

    repeat(maxOf(remoteParts.size, currentParts.size)) { index ->
        val remotePart = remoteParts.getOrElse(index) { 0 }
        val currentPart = currentParts.getOrElse(index) { 0 }
        if (remotePart > currentPart) return true
        if (remotePart < currentPart) return false
    }
    return false
}

private fun String.toVersionParts(): List<Int> =
    trim()
        .removePrefix("v")
        .split('.')
        .map { part -> part.takeWhile(Char::isDigit).toIntOrNull() ?: return emptyList() }
