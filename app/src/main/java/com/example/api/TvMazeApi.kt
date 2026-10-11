package com.example.api

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

@JsonClass(generateAdapter = true)
data class TvMazeSearchResult(
    @Json(name = "score") val score: Double? = null,
    @Json(name = "show") val show: TvMazeShow
)

@JsonClass(generateAdapter = true)
data class TvMazeImage(
    @Json(name = "medium") val medium: String? = null,
    @Json(name = "original") val original: String? = null
)

@JsonClass(generateAdapter = true)
data class TvMazeRating(
    @Json(name = "average") val average: Double? = null
)

@JsonClass(generateAdapter = true)
data class TvMazePerson(
    @Json(name = "name") val name: String,
    @Json(name = "image") val image: TvMazeImage? = null
)

@JsonClass(generateAdapter = true)
data class TvMazeCharacter(
    @Json(name = "name") val name: String
)

@JsonClass(generateAdapter = true)
data class TvMazeCastItem(
    @Json(name = "person") val person: TvMazePerson,
    @Json(name = "character") val character: TvMazeCharacter? = null
)

@JsonClass(generateAdapter = true)
data class TvMazeSeason(
    @Json(name = "id") val id: Long,
    @Json(name = "number") val number: Int,
    @Json(name = "name") val name: String? = null,
    @Json(name = "episodeOrder") val episodeOrder: Int? = null,
    @Json(name = "premiereDate") val premiereDate: String? = null
)

@JsonClass(generateAdapter = true)
data class TvMazeEpisode(
    @Json(name = "id") val id: Long,
    @Json(name = "name") val name: String,
    @Json(name = "season") val season: Int,
    @Json(name = "number") val number: Int?,
    @Json(name = "type") val type: String? = null,
    @Json(name = "airdate") val airdate: String? = null,
    @Json(name = "runtime") val runtime: Int? = null,
    @Json(name = "image") val image: TvMazeImage? = null,
    @Json(name = "summary") val summary: String? = null
)

@JsonClass(generateAdapter = true)
data class TvMazeEmbedded(
    @Json(name = "episodes") val episodes: List<TvMazeEpisode>? = null,
    @Json(name = "seasons") val seasons: List<TvMazeSeason>? = null,
    @Json(name = "cast") val cast: List<TvMazeCastItem>? = null
)

@JsonClass(generateAdapter = true)
data class TvMazeShow(
    @Json(name = "id") val id: Long,
    @Json(name = "name") val name: String,
    @Json(name = "type") val type: String? = null,
    @Json(name = "language") val language: String? = null,
    @Json(name = "genres") val genres: List<String> = emptyList(),
    @Json(name = "status") val status: String? = null,
    @Json(name = "runtime") val runtime: Int? = null,
    @Json(name = "premiered") val premiered: String? = null,
    @Json(name = "rating") val rating: TvMazeRating? = null,
    @Json(name = "image") val image: TvMazeImage? = null,
    @Json(name = "summary") val summary: String? = null,
    @Json(name = "_embedded") val embedded: TvMazeEmbedded? = null
)

interface TvMazeService {
    @GET("search/shows")
    suspend fun searchShows(
        @Query("q") query: String
    ): List<TvMazeSearchResult>

    @GET("shows/{id}")
    suspend fun getShowDetails(
        @Path("id") id: Long,
        @Query("embed[]") embeds: List<String> = listOf("episodes", "seasons", "cast")
    ): TvMazeShow

    @GET("shows/{id}/episodes")
    suspend fun getEpisodes(
        @Path("id") id: Long,
        @Query("specials") specials: Int = 1
    ): List<TvMazeEpisode>

    @GET("shows/{id}/seasons")
    suspend fun getSeasons(
        @Path("id") id: Long
    ): List<TvMazeSeason>

    @GET("shows")
    suspend fun getPopularShows(
        @Query("page") page: Int = 0
    ): List<TvMazeShow>
}
