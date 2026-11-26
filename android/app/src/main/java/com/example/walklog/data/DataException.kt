package com.example.walklog.data

/**
 * data 계층에서 나가는 모든 실패(Result.failure 와 Flow 오류)의 종류. 화면은 Firebase 등 SDK 예외를 보지 않는다.
 *
 * - [NetworkException]: 인터넷이 끊겼거나 서버에 닿지 못함
 * - [AuthException]: 로그인 · 가입 실패
 *   - [InvalidCredentialsException]: 이메일 · 비밀번호가 틀렸거나, 없거나 막힌 계정, 잘못된 Google 토큰
 *   - [InvalidEmailException]: 가입 · 로그인 · 비밀번호 재설정 때 이메일 형식이 틀렸거나 비어 있음
 *   - [EmailAlreadyInUseException]: 이미 가입된 이메일로 가입, 같은 이메일이 다른 방식으로 가입돼 있음
 *   - [WeakPasswordException]: 가입 때 비밀번호가 너무 약하거나 비어 있음
 * - [PermissionDeniedException]: 로그인 안 했거나 풀림, 본인이 아님(남의 uid · 남의 로그), 보안 규칙에 막힘
 * - [NotFoundException]: 고치거나 표시하려는 문서 · 파일이 없음(볼 수 없는 남의 나만 보기 로그 포함)
 * - [TooManyRequestsException]: 너무 자주 시도했거나 사용 한도를 넘음(로그인 시도 반복, Firestore RESOURCE_EXHAUSTED, Storage 한도 초과 등).
 *   잠깐 뒤 또는 한도가 풀린 뒤(무료 하루 한도면 몇 시간 뒤) 다시 시도해야 한다
 * - [LocationPermissionException]: 위치 권한이 없음
 * - [InvalidHandleException] · [HandleTakenException]: 아이디 규칙 위반 · 이미 쓰는 아이디
 * - [PhotoUnreadableException] · [PhotoUploadException]: 사진을 못 읽음 · 사진 업로드 실패
 * - [UnknownDataException]: 위에 없는 실패. 원래 예외는 `cause`에 있다.
 *   cause 가 IllegalArgumentException 이면 잘못된 id 같은 호출 실수라 확실히 실패한 것이므로, 다시 시도해도 같은 결과다
 *
 * `message`는 로그용 영어 문장이라 화면에 그대로 보여 주면 안 된다.
 * 화면은 타입으로 `when` 을 써서 종류마다 strings.xml 문구로 바꾸고, [UnknownDataException]은 일반 오류 문구 하나로 보여 준다.
 */
sealed class DataException(message: String, cause: Throwable? = null) : Exception(message, cause)

class HandleTakenException(val handle: String) : DataException("Handle already taken: $handle")

class InvalidHandleException(val handle: String) : DataException("Invalid handle: $handle")

class PhotoUnreadableException(cause: Throwable? = null) : DataException("Photo could not be read", cause)

class PhotoUploadException(message: String, cause: Throwable? = null) : DataException(message, cause) {
    constructor(cause: Throwable? = null) : this("Photo upload failed", cause)
}

class NetworkException(cause: Throwable?) : DataException("Network unavailable", cause)

sealed class AuthException(message: String, cause: Throwable?) : DataException(message, cause)

class InvalidCredentialsException(cause: Throwable? = null) : AuthException("Invalid email or password", cause)

class InvalidEmailException(cause: Throwable? = null) : AuthException("Malformed email address", cause)

class EmailAlreadyInUseException(cause: Throwable? = null) : AuthException("Email already in use", cause)

class WeakPasswordException(cause: Throwable? = null) : AuthException("Password is too weak", cause)

class PermissionDeniedException(message: String = "Permission denied", cause: Throwable? = null) : DataException(message, cause)

class NotFoundException(message: String = "Not found", cause: Throwable? = null) : DataException(message, cause)

class TooManyRequestsException(cause: Throwable? = null) : DataException("Too many requests", cause)

class LocationPermissionException(cause: Throwable? = null) : DataException("Location permission missing", cause)

class UnknownDataException(cause: Throwable) : DataException("Unknown data error: ${cause.javaClass.simpleName}", cause)
