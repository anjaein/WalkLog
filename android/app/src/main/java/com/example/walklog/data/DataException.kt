package com.example.walklog.data

/**
 * data 계층이 Result.failure 로 돌려주는 예외 종류.
 *
 * `message`는 로그용 영어 문장이라 화면에 그대로 보여 주면 안 된다.
 * 화면은 예외 타입을 보고 strings.xml 의 문구로 바꾸고, 여기 없는 예외는 일반 오류 문구를 보여 준다.
 */
sealed class DataException(message: String, cause: Throwable? = null) : Exception(message, cause)

class HandleTakenException(val handle: String) : DataException("Handle already taken: $handle")

class InvalidHandleException(val handle: String) : DataException("Invalid handle: $handle")

class PhotoUnreadableException(cause: Throwable? = null) : DataException("Photo could not be read", cause)

class PhotoUploadException(message: String, cause: Throwable? = null) : DataException(message, cause) {
    constructor(cause: Throwable? = null) : this("Photo upload failed", cause)
}

class NetworkException(cause: Throwable?) : DataException("Network unavailable", cause)
