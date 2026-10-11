package com.example.api

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

@JsonClass(generateAdapter = true)
data class TmdbGenre(
    @Json(name = "id") val id: Int,
    @Json(name = "name") val name: String
)

@JsonClass(generateAdapter = true)
data class TmdbCast(
    @Json(name = "name") val name: String,
    @Json(name = "character") val character: String? = null,
    @Json(name = "profile_path") val profilePath: String? = null
)

@JsonClass(generateAdapter = true)
data class TmdbCredits(
    @Json(name = "cast") val cast: List<TmdbCast> = emptyList()
)

@JsonClass(generateAdapter = true)
data class TmdbMultiItem(
    @Json(name = "id") val id: Long,
    @Json(name = "title") val title: String? = null,
    @Json(name = "name") val name: String? = null,
    @Json(name = "original_title") val originalTitle: String? = null,
    @Json(name = "original_name") val originalName: String? = null,
    @Json(name = "overview") val overview: String? = null,
    @Json(name = "poster_path") val posterPath: String? = null,
    @Json(name = "backdrop_path") val backdropPath: String? = null,
    @Json(name = "media_type") val mediaType: String? = null, // "movie", "tv"
    @Json(name = "release_date") val releaseDate: String? = null,
    @Json(name = "first_air_date") val firstAirDate: String? = null,
    @Json(name = "vote_average") val voteAverage: Double? = null,
    @Json(name = "genre_ids") val genreIds: List<Int> = emptyList()
)

@JsonClass(generateAdapter = true)
data class TmdbMultiSearchResponse(
    @Json(name = "page") val page: Int = 1,
    @Json(name = "results") val results: List<TmdbMultiItem> = emptyList(),
    @Json(name = "total_results") val totalResults: Int = 0
)

@JsonClass(generateAdapter = true)
data class TmdbEpisode(
    @Json(name = "id") val id: Long,
    @Json(name = "name") val name: String,
    @Json(name = "overview") val overview: String = "",
    @Json(name = "episode_number") val episodeNumber: Int,
    @Json(name = "season_number") val seasonNumber: Int,
    @Json(name = "air_date") val airDate: String? = null,
    @Json(name = "still_path") val stillPath: String? = null,
    @Json(name = "vote_average") val voteAverage: Double? = null
)

@JsonClass(generateAdapter = true)
data class TmdbSeason(
    @Json(name = "id") val id: Long,
    @Json(name = "name") val name: String,
    @Json(name = "season_number") val seasonNumber: Int,
    @Json(name = "episode_count") val episodeCount: Int = 0,
    @Json(name = "poster_path") val posterPath: String? = null,
    @Json(name = "episodes") val episodes: List<TmdbEpisode> = emptyList()
)

@JsonClass(generateAdapter = true)
data class TmdbTvDetail(
    @Json(name = "id") val id: Long,
    @Json(name = "name") val name: String,
    @Json(name = "original_name") val originalName: String? = null,
    @Json(name = "overview") val overview: String? = null,
    @Json(name = "poster_path") val posterPath: String? = null,
    @Json(name = "backdrop_path") val backdropPath: String? = null,
    @Json(name = "first_air_date") val firstAirDate: String? = null,
    @Json(name = "genres") val genres: List<TmdbGenre> = emptyList(),
    @Json(name = "number_of_episodes") val numberOfEpisodes: Int = 0,
    @Json(name = "number_of_seasons") val numberOfSeasons: Int = 0,
    @Json(name = "status") val status: String? = null,
    @Json(name = "vote_average") val voteAverage: Double? = null,
    @Json(name = "seasons") val seasons: List<TmdbSeason> = emptyList(),
    @Json(name = "credits") val credits: TmdbCredits? = null
)

@JsonClass(generateAdapter = true)
data class TmdbMovieDetail(
    @Json(name = "id") val id: Long,
    @Json(name = "title") val title: String,
    @Json(name = "original_title") val originalTitle: String? = null,
    @Json(name = "overview") val overview: String? = null,
    @Json(name = "poster_path") val posterPath: String? = null,
    @Json(name = "backdrop_path") val backdropPath: String? = null,
    @Json(name = "release_date") val releaseDate: String? = null,
    @Json(name = "runtime") val runtime: Int? = null,
    @Json(name = "genres") val genres: List<TmdbGenre> = emptyList(),
    @Json(name = "status") val status: String? = null,
    @Json(name = "vote_average") val voteAverage: Double? = null,
    @Json(name = "credits") val credits: TmdbCredits? = null
)

interface TmdbService {
    @GET("search/multi")
    suspend fun searchMulti(
        @Query("query") query: String,
        @Query("api_key") apiKey: String,
        @Query("language") language: String = "pt-BR",
        @Query("include_adult") includeAdult: Boolean = false
    ): TmdbMultiSearchResponse

    @GET("trending/all/week")
    suspend fun getTrending(
        @Query("api_key") apiKey: String,
        @Query("language") language: String = "pt-BR"
    ): TmdbMultiSearchResponse

    @GET("movie/{movie_id}")
    suspend fun getMovieDetail(
        @Path("movie_id") movieId: Long,
        @Query("api_key") apiKey: String,
        @Query("language") language: String = "pt-BR",
        @Query("append_to_response") append: String = "credits"
    ): TmdbMovieDetail

    @GET("tv/{series_id}")
    suspend fun getTvDetail(
        @Path("series_id") seriesId: Long,
        @Query("api_key") apiKey: String,
        @Query("language") language: String = "pt-BR",
        @Query("append_to_response") append: String = "credits"
    ): TmdbTvDetail

    @GET("tv/{series_id}/season/{season_number}")
    suspend fun getTvSeason(
        @Path("series_id") seriesId: Long,
        @Path("season_number") seasonNumber: Int,
        @Query("api_key") apiKey: String,
        @Query("language") language: String = "pt-BR"
    ): TmdbSeason
}
