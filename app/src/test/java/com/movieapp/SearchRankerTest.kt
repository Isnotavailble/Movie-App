package com.movieapp

import com.movieapp.features.movielist.MovieDTO
import com.movieapp.features.search.SearchRanker
import org.junit.Assert.assertEquals
import org.junit.Test

class SearchRankerTest {

    @Test
    fun exactMatchPrioritizedOverPartialMatches() {
        val candidates = listOf(
            MovieDTO(rawId = 1L, title = "Batman Begins"),
            MovieDTO(rawId = 2L, title = "The Batman"),
            MovieDTO(rawId = 3L, title = "Batman"),
            MovieDTO(rawId = 4L, title = "Batman v Superman")
        )

        val ranked = SearchRanker.rank(candidates, "Batman")
        assertEquals("Batman", ranked[0].displayTitle)
    }

    @Test
    fun wholeWordMatchPrioritizedOverSubstringMatch() {
        val candidates = listOf(
            MovieDTO(rawId = 1L, title = "Gridiron Gang"),
            MovieDTO(rawId = 2L, title = "The Iron Giant"),
            MovieDTO(rawId = 3L, title = "Iron Man"),
            MovieDTO(rawId = 4L, title = "Environment")
        )

        val ranked = SearchRanker.rank(candidates, "Iron")
        
        // "Iron Man" starts with whole word "Iron"
        assertEquals("Iron Man", ranked[0].displayTitle)
        // "The Iron Giant" contains whole word "Iron"
        assertEquals("The Iron Giant", ranked[1].displayTitle)
        // "Environment" (len 11) is closer in length than "Gridiron Gang" (len 13)
        assertEquals("Environment", ranked[2].displayTitle)
        assertEquals("Gridiron Gang", ranked[3].displayTitle)
    }

    @Test
    fun shorterTitlesRankHigherWhenInSameTier() {
        val candidates = listOf(
            MovieDTO(rawId = 1L, title = "Batman: The Long Halloween, Part Two"),
            MovieDTO(rawId = 2L, title = "Batman Begins"),
            MovieDTO(rawId = 3L, title = "Batman")
        )

        val ranked = SearchRanker.rank(candidates, "Batman")
        assertEquals("Batman", ranked[0].displayTitle)
        assertEquals("Batman Begins", ranked[1].displayTitle)
        assertEquals("Batman: The Long Halloween, Part Two", ranked[2].displayTitle)
    }

    @Test
    fun blankQueryPreservesOriginalList() {
        val candidates = listOf(
            MovieDTO(rawId = 1L, title = "Movie A"),
            MovieDTO(rawId = 2L, title = "Movie B")
        )

        val ranked = SearchRanker.rank(candidates, "   ")
        assertEquals(candidates, ranked)
    }
}
