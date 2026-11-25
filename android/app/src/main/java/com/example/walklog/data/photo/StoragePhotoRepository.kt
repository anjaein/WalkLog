package com.example.walklog.data.photo

import android.net.Uri
import com.example.walklog.data.model.LogPhoto
import com.example.walklog.data.suspendRunCatching
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageMetadata
import kotlinx.coroutines.tasks.await
import java.util.UUID

class StoragePhotoRepository(
    private val processor: PhotoProcessor,
    private val storage: FirebaseStorage,
) : PhotoRepository {

    override suspend fun upload(uri: Uri): Result<LogPhoto> = suspendRunCatching {
        val photo = processor.prepare(uri)
        val ref = storage.reference.child("$UPLOAD_FOLDER${UUID.randomUUID()}.jpg")
        val metadata = StorageMetadata.Builder()
            .setContentType("image/jpeg")
            .build()
        ref.putBytes(photo.jpeg, metadata).await()
        val url = ref.downloadUrl.await().toString()
        LogPhoto(url = url, lat = photo.lat, lng = photo.lng, takenAt = photo.takenAt)
    }.wrapUploadFailure()

    override suspend fun delete(photo: LogPhoto): Result<Unit> = suspendRunCatching {
        // getReferenceFromUrl 은 에뮬레이터 주소(http://127.0.0.1:9199/...)를 못 읽는다. 주소 끝의 .../o/{경로} 에서 경로를 꺼낸다
        val path = Uri.parse(photo.url).lastPathSegment
        // 주소는 로그 문서에 저장된 값이라 믿을 수 없다. 우리가 올린 photos/ 밖의 파일은 절대 지우지 않는다
        if (path == null || !path.startsWith(UPLOAD_FOLDER)) return@suspendRunCatching
        storage.reference.child(path).delete().await()
    }

    private companion object {
        const val UPLOAD_FOLDER = "photos/"
    }
}
