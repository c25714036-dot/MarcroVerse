package com.example

import com.example.model.MediaType
import com.example.model.WatchStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun testMediaTypeParsing() {
        assertEquals(MediaType.MOVIE, MediaType.fromString("movie"))
        assertEquals(MediaType.ANIME, MediaType.fromString("anime"))
        assertEquals(MediaType.DORAMA, MediaType.fromString("dorama"))
        assertEquals(MediaType.DOCUMENTARY, MediaType.fromString("documentario"))
        assertEquals(MediaType.SERIES, MediaType.fromString("series"))
    }

    @Test
    fun testWatchStatusEnum() {
        assertEquals(WatchStatus.WANT_TO_WATCH, WatchStatus.fromString("WANT_TO_WATCH"))
        assertEquals(WatchStatus.WATCHING, WatchStatus.fromString("WATCHING"))
        assertEquals(WatchStatus.COMPLETED, WatchStatus.fromString("COMPLETED"))
        assertEquals(WatchStatus.PAUSED, WatchStatus.fromString("PAUSED"))
        assertEquals(WatchStatus.DROPPED, WatchStatus.fromString("DROPPED"))
    }

    @Test
    fun testProgressCalculation() {
        val totalKnownEpisodes = 24
        val watchedCount = 12
        val progress = watchedCount.toFloat() / totalKnownEpisodes.toFloat()
        assertEquals(0.5f, progress, 0.001f)

        // Specials (season 0) logic separation
        val episodes = listOf(
            Triple(0, 1, true),  // Special 1
            Triple(1, 1, false), // Season 1 Ep 1
            Triple(1, 2, false)  // Season 1 Ep 2
        )
        val regularEps = episodes.filter { !it.third && it.first > 0 }
        assertEquals(2, regularEps.size)
    }
}
