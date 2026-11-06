package com.huanchengfly.tieba.post.ui.page.forum.threadlist

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ForumThreadListTypeTest {
    @Test
    fun hot_alwaysUsesSortType3_regardlessOfUserSetting() {
        assertEquals(3, ForumThreadListType.Hot.resolveSortType(0))
        assertEquals(3, ForumThreadListType.Hot.resolveSortType(1))
        assertEquals(3, ForumThreadListType.Hot.resolveSortType(6))
    }

    @Test
    fun latest_passesUserSortTypeThrough() {
        assertEquals(0, ForumThreadListType.Latest.resolveSortType(0))
        assertEquals(1, ForumThreadListType.Latest.resolveSortType(1))
    }

    @Test
    fun good_alwaysUsesMinusOne() {
        assertEquals(-1, ForumThreadListType.Good.resolveSortType(0))
        assertEquals(-1, ForumThreadListType.Good.resolveSortType(1))
    }

    @Test
    fun hot_neverHasMore_evenWhenServerSaysYes() {
        assertFalse(ForumThreadListType.Hot.resolveHasMore(true))
        assertFalse(ForumThreadListType.Hot.resolveHasMore(false))
    }

    @Test
    fun latestAndGood_respectServerHasMore() {
        assertTrue(ForumThreadListType.Latest.resolveHasMore(true))
        assertFalse(ForumThreadListType.Latest.resolveHasMore(false))
        assertTrue(ForumThreadListType.Good.resolveHasMore(true))
        assertFalse(ForumThreadListType.Good.resolveHasMore(false))
    }
}
