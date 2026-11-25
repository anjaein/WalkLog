package com.example.walklog.data.photo

import android.net.Uri
import com.example.walklog.data.DataException
import com.example.walklog.data.NetworkException
import com.example.walklog.data.NotFoundException
import com.example.walklog.data.PermissionDeniedException
import com.example.walklog.data.PhotoUnreadableException
import com.example.walklog.data.PhotoUploadException
import com.example.walklog.data.UnknownDataException
import com.example.walklog.data.model.LogPhoto

/** 모든 실패는 [DataException]이다. */
interface PhotoRepository {
    /**
     * 갤러리 사진을 올린다. 원본에서 찍은 위치 · 시각을 먼저 읽어 [LogPhoto]에 담고,
     * 올리는 파일은 긴 변 1600px JPEG로 줄여서 위치 등 사진 속 정보가 빠지게 한다.
     *
     * 사진을 열거나 읽지 못하면 [PhotoUnreadableException], 네트워크가 안 되면 [NetworkException],
     * 저장소 규칙에 막히면 [PermissionDeniedException], 그 밖에 업로드가 실패하면 [PhotoUploadException]으로 실패한다.
     */
    suspend fun upload(uri: Uri): Result<LogPhoto>

    /**
     * 올린 사진 파일을 지운다. 지울 수 없는 저장소(Cloudinary)는 아무것도 안 하고 성공으로 끝낸다.
     * 앱이 사진을 올리는 폴더 밖을 가리키는 주소면 지우지 않고 성공으로 끝낸다.
     * 네트워크가 안 되면 [NetworkException], 저장소 규칙에 막히면 [PermissionDeniedException],
     * 파일이 없으면 [NotFoundException], 그 밖은 [UnknownDataException]으로 실패한다.
     */
    suspend fun delete(photo: LogPhoto): Result<Unit>
}

/**
 * 종류를 모르는 실패([UnknownDataException])와 업로드에는 맞지 않는 [NotFoundException]만 [PhotoUploadException]으로 바꾼다.
 * 네트워크 · 사진 읽기 · 권한처럼 이미 종류가 정해진 [DataException]은 그대로 둔다.
 */
internal fun <T> Result<T>.wrapUploadFailure(): Result<T> =
    when (val error = exceptionOrNull()) {
        is UnknownDataException -> Result.failure(PhotoUploadException(error.cause))
        is NotFoundException -> Result.failure(PhotoUploadException(error))
        is DataException, null -> this
        else -> Result.failure(PhotoUploadException(error))
    }
