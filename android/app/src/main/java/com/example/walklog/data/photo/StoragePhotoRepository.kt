package com.example.walklog.data.photo

import android.net.Uri
import com.example.walklog.data.model.LogPhoto
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageMetadata
import kotlinx.coroutines.tasks.await
import java.util.UUID

class StoragePhotoRepository(
    private val processor: PhotoProcessor,
    private val storage: FirebaseStorage,
) : PhotoRepository {

    override suspend fun upload(uri: Uri): Result<LogPhoto> = runCatching {
        val photo = processor.prepare(uri)
        val ref = storage.reference.child("photos/${UUID.randomUUID()}.jpg")
        val metadata = StorageMetadata.Builder()
            .setContentType("image/jpeg")
            .build()
        ref.putBytes(photo.jpeg, metadata).await()
        val url = ref.downloadUrl.await().toString()
        LogPhoto(url = url, lat = photo.lat, lng = photo.lng, takenAt = photo.takenAt)
    }

    override suspend fun delete(photo: LogPhoto): Result<Unit> = runCatching {
        storage.getReferenceFromUrl(photo.url).delete().await()
        Unit
    }
}
