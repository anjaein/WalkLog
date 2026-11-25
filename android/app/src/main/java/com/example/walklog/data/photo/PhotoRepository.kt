package com.example.walklog.data.photo

import android.net.Uri
import com.example.walklog.data.model.LogPhoto

interface PhotoRepository {
    /**
     * 갤러리 사진을 올린다. 원본에서 찍은 위치 · 시각을 먼저 읽어 [LogPhoto]에 담고,
     * 올리는 파일은 긴 변 1600px JPEG로 줄여서 위치 등 사진 속 정보가 빠지게 한다.
     */
    suspend fun upload(uri: Uri): Result<LogPhoto>

    /** 올린 사진 파일을 지운다. 지울 수 없는 저장소(Cloudinary)는 아무것도 안 하고 성공으로 끝낸다. */
    suspend fun delete(photo: LogPhoto): Result<Unit>
}
