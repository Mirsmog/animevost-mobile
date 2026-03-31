package com.animevost.app.core.domain.util

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter

// Minimum query length matches the threshold used across HomeViewModel and SearchViewModel.
private const val MIN_QUERY_LENGTH = 4

fun Flow<String>.asSearchQuery(debounceMillis: Long = 300L): Flow<String> =
    this
        .debounce(debounceMillis)
        .distinctUntilChanged()
        .filter { it.isBlank() || it.trim().length >= MIN_QUERY_LENGTH }
