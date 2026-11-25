package com.example.walklog.data.photo

import android.net.Uri
import com.example.walklog.data.PhotoUploadException
import com.example.walklog.data.model.LogPhoto
import com.example.walklog.data.suspendRunCatching
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID

class CloudinaryPhotoRepository(
    private val processor: PhotoProcessor,
    private val cloudName: String,
    private val uploadPreset: String,
) : PhotoRepository {

    override suspend fun upload(uri: Uri): Result<LogPhoto> = suspendRunCatching {
        if (cloudName.isBlank() || uploadPreset.isBlank()) {
            throw PhotoUploadException("Cloudinary cloudName or uploadPreset is blank. Check local.properties.")
        }
        val photo = processor.prepare(uri)
        val url = withContext(Dispatchers.IO) { postImage(photo.jpeg) }
        LogPhoto(url = url, lat = photo.lat, lng = photo.lng, takenAt = photo.takenAt)
    }.wrapUploadFailure()

    // 업로드한 파일을 지우려면 API secret으로 서명해야 하는데, secret은 앱에 넣으면 안 되므로 지우지 않는다.
    override suspend fun delete(photo: LogPhoto): Result<Unit> = Result.success(Unit)

    private suspend fun postImage(jpeg: ByteArray): String {
        val boundary = "WalkLog-${UUID.randomUUID()}"
        val body = multipartBody(boundary, jpeg)
        val connection = URL("https://api.cloudinary.com/v1_1/$cloudName/image/upload")
            .openConnection() as HttpURLConnection
        try {
            // HttpURLConnection 은 막혀 있는 동안 코루틴 취소도 스레드 인터럽트도 듣지 않는다.
            // 요청은 자식 코루틴에서 돌리고, 기다리던 쪽이 취소되면 연결을 끊어 막힌 읽기 · 쓰기를 깨운다
            return coroutineScope {
                val response = async { send(connection, boundary, body, checkActive = { ensureActive() }) }
                try {
                    response.await()
                } catch (e: CancellationException) {
                    connection.disconnect()
                    throw e
                }
            }
        } finally {
            connection.disconnect()
        }
    }

    private fun send(
        connection: HttpURLConnection,
        boundary: String,
        body: ByteArray,
        checkActive: () -> Unit,
    ): String {
        connection.requestMethod = "POST"
        connection.doOutput = true
        connection.connectTimeout = TIMEOUT_MS
        connection.readTimeout = TIMEOUT_MS
        connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=$boundary")
        // 길이를 미리 알려 주지 않으면 HttpURLConnection 이 본문 전체를 한 번 더 메모리에 모아 둔다
        connection.setFixedLengthStreamingMode(body.size)

        // 연결이 열리기 전에 부른 disconnect() 는 아무 일도 하지 않아서, 그 사이에 취소됐는지 여기서 한 번 더 본다
        checkActive()
        connection.outputStream.use { it.write(body) }

        val code = connection.responseCode
        val stream = if (code in 200..299) connection.inputStream else connection.errorStream
        val text = stream?.bufferedReader()?.use { it.readText() }.orEmpty()

        if (code !in 200..299) {
            val message = runCatching {
                JSONObject(text).optJSONObject("error")?.optString("message")
            }.getOrNull()
            throw PhotoUploadException("Cloudinary upload failed ($code): ${message?.takeIf { it.isNotBlank() } ?: text}")
        }

        val secureUrl = JSONObject(text).optString("secure_url")
        if (secureUrl.isBlank()) throw PhotoUploadException("Cloudinary response has no secure_url: $text")
        return secureUrl
    }

    private fun multipartBody(boundary: String, jpeg: ByteArray): ByteArray {
        val out = ByteArrayOutputStream(jpeg.size + MULTIPART_OVERHEAD)
        fun text(value: String) = out.write(value.toByteArray(Charsets.UTF_8))

        text("--$boundary\r\n")
        text("Content-Disposition: form-data; name=\"upload_preset\"\r\n\r\n")
        text(uploadPreset)
        text("\r\n")

        text("--$boundary\r\n")
        text("Content-Disposition: form-data; name=\"file\"; filename=\"photo.jpg\"\r\n")
        text("Content-Type: image/jpeg\r\n\r\n")
        out.write(jpeg)
        text("\r\n")

        text("--$boundary--\r\n")
        return out.toByteArray()
    }

    private companion object {
        const val TIMEOUT_MS = 20_000
        const val MULTIPART_OVERHEAD = 512
    }
}
