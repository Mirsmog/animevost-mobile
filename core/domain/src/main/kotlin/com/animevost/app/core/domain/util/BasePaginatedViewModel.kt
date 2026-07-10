package com.animevost.app.core.domain.util

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Base ViewModel providing common pagination state and logic.
 * Subclasses implement [fetchPage] to supply data for each page.
 */
abstract class BasePaginatedViewModel<T> : ViewModel() {

    protected val _items = MutableStateFlow<List<T>>(emptyList())
    val items: StateFlow<List<T>> = _items.asStateFlow()

    protected val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    protected val _isLoadingMore = MutableStateFlow(false)
    val isLoadingMore: StateFlow<Boolean> = _isLoadingMore.asStateFlow()

    protected val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    protected val _hasMore = MutableStateFlow(true)
    val hasMore: StateFlow<Boolean> = _hasMore.asStateFlow()

    protected var currentPage = 1
    private var loadInitialJob: Job? = null
    private var loadMoreJob: Job? = null

    /**
     * Fetch items for the given [page]. Return the list of items.
     * Return empty list to signal end of pagination.
     * Throw (non-[CancellationException]) to signal an error.
     */
    protected abstract suspend fun fetchPage(page: Int): List<T>

    /**
     * Merge [existing] items with [new] items when loading more pages.
     * Override to apply deduplication or custom merge logic.
     */
    protected open fun mergeItems(existing: List<T>, new: List<T>): List<T> = existing + new

    /** Reset to page 1 and load fresh data, cancelling any in-flight load. */
    protected fun loadInitial() {
        loadInitialJob?.cancel()
        loadMoreJob?.cancel()
        currentPage = 1
        _hasMore.value = true
        _error.value = null
        _isLoading.value = true
        _isLoadingMore.value = false
        loadInitialJob = viewModelScope.launch {
            try {
                val result = fetchPage(currentPage)
                _items.value = result
                _hasMore.value = result.isNotEmpty()
                _isLoading.value = false
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _error.value = e.message ?: "Unknown error"
                _isLoading.value = false
            }
        }
    }

    /** Cancel any in-flight initial load and reset the loading flag. */
    protected fun cancelLoad() {
        loadInitialJob?.cancel()
        loadMoreJob?.cancel()
        _isLoading.value = false
        _isLoadingMore.value = false
    }

    fun loadMore() {
        if (_isLoading.value || _isLoadingMore.value || !_hasMore.value) return
        _isLoadingMore.value = true
        loadMoreJob = viewModelScope.launch {
            try {
                val result = fetchPage(currentPage + 1)
                if (result.isNotEmpty()) {
                    currentPage++
                    _items.update { mergeItems(it, result) }
                } else {
                    _hasMore.value = false
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _error.value = e.message ?: "Unknown error"
            } finally {
                _isLoadingMore.value = false
            }
        }
    }

    fun refresh() = loadInitial()
}
