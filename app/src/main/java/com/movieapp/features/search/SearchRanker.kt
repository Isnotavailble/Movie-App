package com.movieapp.features.search

import com.movieapp.features.movielist.MovieDTO
import java.util.Locale
import kotlin.math.abs

/**
 * Intelligent relevance scoring and ranking utility for movie/TV search results.
 * Prioritizes exact matches, whole-word occurrences, and shortest character distance
 * over general partial substring matches.
 */
object SearchRanker {

    private const val SCORE_EXACT_MATCH = 1000
    private const val SCORE_STARTS_WITH_WHOLE_WORD = 800
    private const val SCORE_WHOLE_WORD_ANYWHERE = 600
    private const val SCORE_STARTS_WITH_PREFIX = 500
    private const val SCORE_WORD_PREFIX = 400
    private const val SCORE_CONTAINS = 200
    private const val SCORE_FALLBACK = 100

    /**
     * Ranks a list of [MovieDTO] items against a user search query.
     *
     * @param items Raw search results from network or cache.
     * @param query The user's input search string.
     * @return Re-ordered list with most relevant whole-word and closest matches first.
     */
    fun rank(items: List<MovieDTO>, query: String): List<MovieDTO> {
        val cleanQuery = query.trim().lowercase(Locale.ROOT)
        if (cleanQuery.isBlank() || items.isEmpty()) {
            return items
        }

        val wordBoundaryRegex = try {
            Regex("\\b${Regex.escape(cleanQuery)}\\b", RegexOption.IGNORE_CASE)
        } catch (_: Exception) {
            null
        }

        return items
            .mapIndexed { originalIndex, movie ->
                val score = calculateScore(movie, cleanQuery, wordBoundaryRegex)
                val lengthDiff = abs(movie.displayTitle.trim().length - cleanQuery.length)
                RankedItem(movie = movie, score = score, lengthDiff = lengthDiff, originalIndex = originalIndex)
            }
            .sortedWith(
                compareByDescending<RankedItem> { it.score }
                    .thenBy { it.lengthDiff }
                    .thenBy { it.originalIndex }
            )
            .map { it.movie }
    }

    private fun calculateScore(
        movie: MovieDTO,
        cleanQuery: String,
        wordBoundaryRegex: Regex?
    ): Int {
        val title = movie.displayTitle.trim()
        val lowerTitle = title.lowercase(Locale.ROOT)

        // 1. Exact match (case-insensitive)
        if (lowerTitle == cleanQuery) {
            return SCORE_EXACT_MATCH
        }

        // 2. Starts with query as a whole word (e.g. "Iron Man" for query "iron")
        if (lowerTitle.startsWith(cleanQuery)) {
            val nextCharIndex = cleanQuery.length
            if (nextCharIndex >= lowerTitle.length || !lowerTitle[nextCharIndex].isLetterOrDigit()) {
                return SCORE_STARTS_WITH_WHOLE_WORD
            }
        }

        // 3. Contains query as a standalone whole word anywhere (e.g. "The Iron Giant" for query "iron")
        if (wordBoundaryRegex != null && wordBoundaryRegex.containsMatchIn(title)) {
            return SCORE_WHOLE_WORD_ANYWHERE
        }

        // 4. Starts with prefix substring (e.g. "Batman" for query "bat")
        if (lowerTitle.startsWith(cleanQuery)) {
            return SCORE_STARTS_WITH_PREFIX
        }

        // 5. Any word inside title starts with query (e.g. "Spider-Man: Across the Spider-Verse" for query "across")
        val words = lowerTitle.split(Regex("[\\s\\-_:;,./\\\\]+"))
        if (words.any { it.startsWith(cleanQuery) }) {
            return SCORE_WORD_PREFIX
        }

        // 6. Substring contains anywhere
        if (lowerTitle.contains(cleanQuery)) {
            return SCORE_CONTAINS
        }

        // 7. Fallback (e.g. matched on slug or category from backend)
        return SCORE_FALLBACK
    }

    private data class RankedItem(
        val movie: MovieDTO,
        val score: Int,
        val lengthDiff: Int,
        val originalIndex: Int
    )
}
