package com.animevost.app.core.domain.util

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class BasePaginatedViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `refresh cancels stale load more result`() = runTest(dispatcher) {
        val viewModel = TestViewModel()
        viewModel.firstPage = listOf("old-first")

        viewModel.refresh()
        runCurrent()
        assertEquals(listOf("old-first"), viewModel.items.value)

        viewModel.loadMore()
        runCurrent()
        assertTrue(viewModel.isLoadingMore.value)

        viewModel.firstPage = listOf("new-first")
        viewModel.refresh()
        runCurrent()
        assertEquals(listOf("new-first"), viewModel.items.value)
        assertFalse(viewModel.isLoadingMore.value)

        viewModel.secondPage.complete(listOf("stale-second"))
        runCurrent()

        assertEquals(listOf("new-first"), viewModel.items.value)
    }

    private class TestViewModel : BasePaginatedViewModel<String>() {
        var firstPage: List<String> = emptyList()
        val secondPage = CompletableDeferred<List<String>>()

        override suspend fun fetchPage(page: Int): List<String> =
            if (page == 1) firstPage else secondPage.await()
    }
}
