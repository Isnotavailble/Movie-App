package com.movieapp

import com.movieapp.features.moviedetail.CategoryDTO
import com.movieapp.features.movielist.MediaCategory
import com.movieapp.features.movielist.MovieDTO
import com.movieapp.features.movielist.MovieListUiState
import com.movieapp.features.movielist.MovieSortOrder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FilterAndSortUnitTest {

    private val actionMovie = MovieDTO(
        rawId = 1L,
        title = "Spider-Man: Across the Spider-Verse",
        releaseYear = "2023",
        rawRating = 8.7,
        categories = listOf(CategoryDTO(1, "Action"), CategoryDTO(2, "Animation"))
    )

    private val comedyMovie = MovieDTO(
        rawId = 2L,
        title = "Barbie",
        releaseYear = "2023",
        rawRating = 6.9,
        categories = listOf(CategoryDTO(3, "Comedy"), CategoryDTO(4, "Fantasy"))
    )

    private val dramaMovie = MovieDTO(
        rawId = 3L,
        title = "Oppenheimer",
        releaseYear = "2023",
        rawRating = 8.9,
        categories = listOf(CategoryDTO(5, "Drama"), CategoryDTO(6, "History"))
    )

    private val classicMovie = MovieDTO(
        rawId = 4L,
        title = "The Dark Knight",
        releaseYear = "2008",
        rawRating = 9.0,
        categories = listOf(CategoryDTO(1, "Action"), CategoryDTO(7, "Crime"), CategoryDTO(5, "Drama"))
    )

    private val lowRatedMovie = MovieDTO(
        rawId = 5L,
        title = "Low Rated Movie",
        releaseYear = "2020",
        rawRating = 5.2,
        categories = listOf(CategoryDTO(1, "Action"))
    )

    private val sampleMovies = listOf(actionMovie, comedyMovie, dramaMovie, classicMovie, lowRatedMovie)

    @Test
    fun filterByGenre_returnsOnlyMatchingMovies() {
        val state = MovieListUiState(
            activeCategory = MediaCategory.MOVIES,
            movies = sampleMovies,
            selectedGenre = "Drama"
        )

        val result = state.currentDisplayList
        assertEquals(2, result.size)
        assertTrue(result.any { it.displayTitle == "Oppenheimer" })
        assertTrue(result.any { it.displayTitle == "The Dark Knight" })
        assertFalse(result.any { it.displayTitle == "Barbie" })
    }

    @Test
    fun filterByGenre_nullOrAll_returnsAllMovies() {
        val stateNull = MovieListUiState(
            activeCategory = MediaCategory.MOVIES,
            movies = sampleMovies,
            selectedGenre = null
        )
        assertEquals(sampleMovies.size, stateNull.currentDisplayList.size)
    }

    @Test
    fun filterByRating_startingFrom6_filtersLowerRatedMovies() {
        // Rating 6+
        val state6 = MovieListUiState(
            activeCategory = MediaCategory.MOVIES,
            movies = sampleMovies,
            minRating = 6.0
        )
        val result6 = state6.currentDisplayList
        assertEquals(4, result6.size)
        assertFalse(result6.any { (it.rating ?: 0.0) < 6.0 })

        // Rating 7+
        val state7 = MovieListUiState(
            activeCategory = MediaCategory.MOVIES,
            movies = sampleMovies,
            minRating = 7.0
        )
        val result7 = state7.currentDisplayList
        assertEquals(3, result7.size)
        assertFalse(result7.any { (it.rating ?: 0.0) < 7.0 })

        // Rating 8+
        val state8 = MovieListUiState(
            activeCategory = MediaCategory.MOVIES,
            movies = sampleMovies,
            minRating = 8.0
        )
        val result8 = state8.currentDisplayList
        assertEquals(3, result8.size)
        assertTrue(result8.all { (it.rating ?: 0.0) >= 8.0 })
    }

    @Test
    fun combinedFilter_genreAndRating() {
        val state = MovieListUiState(
            activeCategory = MediaCategory.MOVIES,
            movies = sampleMovies,
            selectedGenre = "Action",
            minRating = 8.0
        )

        val result = state.currentDisplayList
        // Action movies are: Spider-Man (8.7), The Dark Knight (9.0), Low Rated Movie (5.2)
        // With minRating >= 8.0: Spider-Man and The Dark Knight should pass
        assertEquals(2, result.size)
        assertTrue(result.any { it.displayTitle == "Spider-Man: Across the Spider-Verse" })
        assertTrue(result.any { it.displayTitle == "The Dark Knight" })
        assertFalse(result.any { it.displayTitle == "Low Rated Movie" })
    }

    @Test
    fun sortOrder_topRated_ordersDescendingByRating() {
        val state = MovieListUiState(
            activeCategory = MediaCategory.MOVIES,
            movies = sampleMovies,
            sortOrder = MovieSortOrder.TOP_RATED
        )

        val result = state.currentDisplayList
        assertEquals("The Dark Knight", result[0].displayTitle) // 9.0
        assertEquals("Oppenheimer", result[1].displayTitle) // 8.9
        assertEquals("Spider-Man: Across the Spider-Verse", result[2].displayTitle) // 8.7
        assertEquals("Barbie", result[3].displayTitle) // 6.9
        assertEquals("Low Rated Movie", result[4].displayTitle) // 5.2
    }

    @Test
    fun sortOrder_newest_ordersDescendingByReleaseYear() {
        val state = MovieListUiState(
            activeCategory = MediaCategory.MOVIES,
            movies = sampleMovies,
            sortOrder = MovieSortOrder.NEWEST
        )

        val result = state.currentDisplayList
        // 2023 movies come first, then 2020, then 2008
        assertEquals("2023", result[0].displayYear)
        assertEquals("The Dark Knight", result.last().displayTitle) // 2008
    }

    @Test
    fun sortOrder_titleAz_ordersAlphabetically() {
        val state = MovieListUiState(
            activeCategory = MediaCategory.MOVIES,
            movies = sampleMovies,
            sortOrder = MovieSortOrder.TITLE_AZ
        )

        val result = state.currentDisplayList
        assertEquals("Barbie", result[0].displayTitle)
        assertEquals("Low Rated Movie", result[1].displayTitle)
        assertEquals("Oppenheimer", result[2].displayTitle)
        assertEquals("Spider-Man: Across the Spider-Verse", result[3].displayTitle)
        assertEquals("The Dark Knight", result[4].displayTitle)
    }

    @Test
    fun filterEmptyState_detectedWhenNoMatchesFound() {
        val state = MovieListUiState(
            activeCategory = MediaCategory.MOVIES,
            movies = sampleMovies,
            selectedGenre = "Horror", // No horror in sample
            isInitialLoading = false
        )

        assertTrue(state.isFilterActive)
        assertTrue(state.currentDisplayList.isEmpty())
        assertTrue(state.isFilterEmpty)
    }

    @Test
    fun availableGenres_includesExtractedGenresAndDefaults() {
        val state = MovieListUiState(
            activeCategory = MediaCategory.MOVIES,
            movies = sampleMovies
        )

        val genres = state.availableGenres
        assertTrue(genres.contains("Action"))
        assertTrue(genres.contains("Comedy"))
        assertTrue(genres.contains("Drama"))
        assertTrue(genres.contains("History"))
        assertTrue(genres.contains("Sci-Fi")) // from fallback defaults
    }
}
