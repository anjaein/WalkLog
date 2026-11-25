package com.example.walklog.data.photo

import android.net.Uri
import com.example.walklog.data.DataException
import com.example.walklog.data.NetworkException
import com.example.walklog.data.PhotoUnreadableException
import com.example.walklog.data.PhotoUploadException
import com.example.walklog.data.model.LogPhoto

interface PhotoRepository {
    /**
     * 갤러리 사진을 올린다. 원본에서 찍은 위치 · 시각을 먼저 읽어 [LogPhoto]에 담고,
     * 올리는 파일은 긴 변 1600px JPEG로 줄여서 위치 등 사진 속 정보가 빠지게 한다.
     *
     * 사진을 열거나 읽지 못하면 [PhotoUnreadableException], 네트워크가 안 되면 [NetworkException],
     * 그 밖에 업로드가 실패하면 [PhotoUploadException]으로 실패한다.
     */
    suspend fun upload(uri: Uri): Result<LogPhoto>

    /**
     * 올린 사진 파일을 지운다. 지울 수 없는 저장소(Cloudinary)는 아무것도 안 하고 성공으로 끝낸다.
     * 앱이 사진을 올리는 폴더 밖을 가리키는 주소면 지우지 않고 성공으로 끝낸다.
     */
    suspend fun delete(photo: LogPhoto): Result<Unit>
}

/** 이미 종류가 정해진 실패([DataException])는 그대로 두고, 나머지 업로드 실패는 [PhotoUploadException]으로 감싼다. */
internal fun <T> Result<T>.wrapUploadFailure(): Result<T> {
    val error = exceptionOrNull() ?: return this
    return if (error is DataException) this else Result.failure(PhotoUploadException(error))
}
