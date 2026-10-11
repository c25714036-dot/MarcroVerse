package com.example.data

import android.content.Context
import android.util.Log
import com.example.R
import com.example.model.EpisodeDetail
import com.example.model.EpisodeProgress
import com.example.model.LibraryItem
import com.example.model.SeasonDetail
import com.example.model.UserReview
import com.example.model.WatchStatus
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class FirestoreRepository(
    private val db: FirebaseFirestore
) {
    // Convenience constructor reading custom database ID
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    fun observeLibrary(userId: String): Flow<List<LibraryItem>> {
        require(userId.isNotBlank()) { "UserId must not be blank" }
        return db.collection("users").document(userId)
            .collection("library")
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(LibraryItem::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.updatedAt ?: it.createdAt ?: Timestamp(0, 0) }
            }
    }

    fun observeItem(userId: String, mediaId: String): Flow<LibraryItem?> {
        require(userId.isNotBlank()) { "UserId must not be blank" }
        require(mediaId.isNotBlank()) { "MediaId must not be blank" }
        return db.collection("users").document(userId)
            .collection("library").document(mediaId)
            .snapshots()
            .map { snapshot ->
                if (snapshot.exists()) {
                    snapshot.toObject(LibraryItem::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                } else null
            }
    }

    fun observeEpisodeProgress(userId: String, mediaId: String): Flow<Map<String, EpisodeProgress>> = callbackFlow {
        require(userId.isNotBlank()) { "UserId must not be blank" }
        require(mediaId.isNotBlank()) { "MediaId must not be blank" }

        val listener = db.collection("users").document(userId)
            .collection("library").document(mediaId)
            .collection("episodes")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w(TAG, "Error observing episodes: ${error.message}")
                    close(error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val map = snapshot.documents.mapNotNull { doc ->
                        val ep = doc.toObject(EpisodeProgress::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        if (ep != null) doc.id to ep else null
                    }.toMap()
                    trySend(map)
                }
            }

        awaitClose { listener.remove() }
    }

    fun observeReviews(userId: String): Flow<List<UserReview>> {
        require(userId.isNotBlank()) { "UserId must not be blank" }
        return db.collection("users").document(userId)
            .collection("reviews")
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(UserReview::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.updatedAt ?: it.createdAt ?: Timestamp(0, 0) }
            }
    }

    suspend fun saveToLibrary(userId: String, item: LibraryItem) {
        require(userId.isNotBlank()) { "UserId must not be blank" }
        require(item.id.isNotBlank()) { "Item id must not be blank" }

        val docRef = db.collection("users").document(userId)
            .collection("library").document(item.id)

        val existing = docRef.get().await()
        val data = mutableMapOf<String, Any?>(
            "id" to item.id,
            "userId" to userId,
            "externalId" to item.externalId,
            "source" to item.source,
            "mediaType" to item.mediaType,
            "title" to item.title,
            "originalTitle" to item.originalTitle,
            "overview" to item.overview,
            "posterPath" to item.posterPath,
            "backdropPath" to item.backdropPath,
            "releaseYear" to item.releaseYear,
            "genres" to item.genres,
            "status" to item.status,
            "totalEpisodes" to item.totalEpisodes,
            "watchedEpisodes" to (if (existing.exists()) (existing.getLong("watchedEpisodes")?.toInt() ?: item.watchedEpisodes) else item.watchedEpisodes),
            "totalSeasons" to item.totalSeasons,
            "isMovie" to item.isMovie,
            "movieWatched" to item.movieWatched,
            "userRating" to item.userRating,
            "reviewText" to item.reviewText,
            "updatedAt" to FieldValue.serverTimestamp()
        )

        if (!existing.exists()) {
            data["createdAt"] = FieldValue.serverTimestamp()
        }

        docRef.set(data.filterValues { it != null }).await()
    }

    suspend fun updateStatus(userId: String, mediaId: String, status: WatchStatus) {
        require(userId.isNotBlank()) { "UserId must not be blank" }
        require(mediaId.isNotBlank()) { "MediaId must not be blank" }

        val docRef = db.collection("users").document(userId)
            .collection("library").document(mediaId)

        docRef.update(
            mapOf(
                "status" to status.name,
                "updatedAt" to FieldValue.serverTimestamp()
            )
        ).await()
    }

    suspend fun toggleMovieWatched(userId: String, mediaId: String, watched: Boolean) {
        require(userId.isNotBlank()) { "UserId must not be blank" }
        require(mediaId.isNotBlank()) { "MediaId must not be blank" }

        val docRef = db.collection("users").document(userId)
            .collection("library").document(mediaId)

        val newStatus = if (watched) WatchStatus.COMPLETED.name else WatchStatus.WATCHING.name
        docRef.update(
            mapOf(
                "movieWatched" to watched,
                "status" to newStatus,
                "updatedAt" to FieldValue.serverTimestamp()
            )
        ).await()
    }

    suspend fun setEpisodeWatched(
        userId: String,
        mediaId: String,
        seasonNumber: Int,
        episodeNumber: Int,
        episodeTitle: String,
        isSpecial: Boolean,
        watched: Boolean,
        seasons: List<SeasonDetail>?
    ) {
        require(userId.isNotBlank()) { "UserId must not be blank" }
        require(mediaId.isNotBlank()) { "MediaId must not be blank" }

        val epKey = "s${seasonNumber}_e${episodeNumber}"
        val epDocRef = db.collection("users").document(userId)
            .collection("library").document(mediaId)
            .collection("episodes").document(epKey)

        val epData = mapOf(
            "id" to epKey,
            "userId" to userId,
            "mediaId" to mediaId,
            "seasonNumber" to seasonNumber,
            "episodeNumber" to episodeNumber,
            "title" to episodeTitle,
            "isSpecial" to isSpecial,
            "watched" to watched,
            "updatedAt" to FieldValue.serverTimestamp()
        )
        epDocRef.set(epData).await()

        // Recalculate progress across library item
        recalculateProgress(userId, mediaId, seasons)
    }

    suspend fun setSeasonWatched(
        userId: String,
        mediaId: String,
        seasonNumber: Int,
        episodes: List<EpisodeDetail>,
        watched: Boolean,
        seasons: List<SeasonDetail>?
    ) {
        require(userId.isNotBlank()) { "UserId must not be blank" }
        require(mediaId.isNotBlank()) { "MediaId must not be blank" }

        val batch = db.batch()
        episodes.forEach { ep ->
            val epKey = "s${ep.seasonNumber}_e${ep.episodeNumber}"
            val ref = db.collection("users").document(userId)
                .collection("library").document(mediaId)
                .collection("episodes").document(epKey)

            batch.set(
                ref,
                mapOf(
                    "id" to epKey,
                    "userId" to userId,
                    "mediaId" to mediaId,
                    "seasonNumber" to ep.seasonNumber,
                    "episodeNumber" to ep.episodeNumber,
                    "title" to ep.name,
                    "isSpecial" to ep.isSpecial,
                    "watched" to watched,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
        }
        batch.commit().await()

        recalculateProgress(userId, mediaId, seasons)
    }

    private suspend fun recalculateProgress(
        userId: String,
        mediaId: String,
        seasons: List<SeasonDetail>?
    ) {
        val libraryDocRef = db.collection("users").document(userId)
            .collection("library").document(mediaId)

        val episodesSnapshot = libraryDocRef.collection("episodes").get().await()
        val watchedList = episodesSnapshot.documents.mapNotNull { doc ->
            doc.toObject(EpisodeProgress::class.java)
        }.filter { it.watched }

        val nonSpecialWatchedCount = watchedList.count { !it.isSpecial && it.seasonNumber > 0 }

        // Determine last watched and next unwatched episode
        val lastWatched = watchedList.filter { !it.isSpecial }
            .maxWithOrNull(compareBy({ it.seasonNumber }, { it.episodeNumber }))

        var nextSeason: Int? = null
        var nextEpisode: Int? = null
        var nextName: String? = null

        if (seasons != null && seasons.isNotEmpty()) {
            val watchedSet = watchedList.map { "s${it.seasonNumber}_e${it.episodeNumber}" }.toSet()
            val allEpisodesOrdered = seasons.filter { it.seasonNumber > 0 }
                .flatMap { it.episodes.filter { ep -> !ep.isSpecial } }
                .sortedWith(compareBy({ it.seasonNumber }, { it.episodeNumber }))

            val next = allEpisodesOrdered.firstOrNull { ep ->
                !watchedSet.contains("s${ep.seasonNumber}_e${ep.episodeNumber}")
            }
            if (next != null) {
                nextSeason = next.seasonNumber
                nextEpisode = next.episodeNumber
                nextName = next.name
            }
        }

        val libSnap = libraryDocRef.get().await()
        val totalKnownEps = libSnap.getLong("totalEpisodes")?.toInt() ?: 0

        val newStatus = when {
            totalKnownEps > 0 && nonSpecialWatchedCount >= totalKnownEps -> WatchStatus.COMPLETED.name
            nonSpecialWatchedCount > 0 -> WatchStatus.WATCHING.name
            else -> libSnap.getString("status") ?: WatchStatus.WANT_TO_WATCH.name
        }

        val updateData = mutableMapOf<String, Any?>(
            "watchedEpisodes" to nonSpecialWatchedCount,
            "status" to newStatus,
            "lastWatchedSeason" to lastWatched?.seasonNumber,
            "lastWatchedEpisode" to lastWatched?.episodeNumber,
            "lastWatchedEpisodeName" to lastWatched?.title,
            "nextEpisodeSeason" to nextSeason,
            "nextEpisodeNumber" to nextEpisode,
            "nextEpisodeName" to nextName,
            "updatedAt" to FieldValue.serverTimestamp()
        )

        libraryDocRef.update(updateData.filterValues { it != null }).await()
    }

    suspend fun saveReview(userId: String, review: UserReview) {
        require(userId.isNotBlank()) { "UserId must not be blank" }
        require(review.id.isNotBlank()) { "Review id (mediaId) must not be blank" }

        val docRef = db.collection("users").document(userId)
            .collection("reviews").document(review.id)

        val existing = docRef.get().await()
        val data = mutableMapOf<String, Any?>(
            "id" to review.id,
            "userId" to userId,
            "mediaId" to review.mediaId,
            "mediaTitle" to review.mediaTitle,
            "mediaType" to review.mediaType,
            "posterPath" to review.posterPath,
            "rating" to review.rating,
            "review" to review.review,
            "updatedAt" to FieldValue.serverTimestamp()
        )
        if (!existing.exists()) {
            data["createdAt"] = FieldValue.serverTimestamp()
        }

        docRef.set(data.filterValues { it != null }).await()

        // Also update userRating and reviewText on Library item if present
        try {
            val libRef = db.collection("users").document(userId)
                .collection("library").document(review.mediaId)
            libRef.update(
                mapOf(
                    "userRating" to review.rating,
                    "reviewText" to review.review,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (e: Exception) {
            Log.d(TAG, "Library item not present for review update: ${e.message}")
        }
    }

    suspend fun deleteReview(userId: String, mediaId: String) {
        require(userId.isNotBlank()) { "UserId must not be blank" }
        require(mediaId.isNotBlank()) { "MediaId must not be blank" }

        db.collection("users").document(userId)
            .collection("reviews").document(mediaId)
            .delete().await()

        try {
            db.collection("users").document(userId)
                .collection("library").document(mediaId)
                .update(
                    mapOf(
                        "userRating" to FieldValue.delete(),
                        "reviewText" to FieldValue.delete(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
        } catch (e: Exception) {
            // Ignore if library item doesn't exist
        }
    }

    suspend fun removeFromLibrary(userId: String, mediaId: String) {
        require(userId.isNotBlank()) { "UserId must not be blank" }
        require(mediaId.isNotBlank()) { "MediaId must not be blank" }

        db.collection("users").document(userId)
            .collection("library").document(mediaId)
            .delete().await()
    }

    companion object {
        private const val TAG = "FirestoreRepo"
    }
}
