package com.example.walklog.data.photo

import android.net.Uri
import com.example.walklog.data.model.LogPhoto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.DataOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID

class CloudinaryPhotoRepository(
    private val processor: PhotoProcessor,
    private val cloudName: String,
    private val uploadPreset: String,
) : PhotoRepository {

    override suspend fun upload(uri: Uri): Result<LogPhoto> = runCatching {
        check(cloudName.isNotBlank() && uploadPreset.isNotBlank()) {
            "Cloudinary cloudName · uploadPreset이 비어 있습니다. local.properties 값을 확인하세요."
        }
        val photo = processor.prepare(uri)
        val url = withContext(Dispatchers.IO) { postImage(photo.jpeg) }
        LogPhoto(url = url, lat = photo.lat, lng = photo.lng, takenAt = photo.takenAt)
    }

    // 업로드한 파일을 지우려면 API secret으로 서명해야 하는데, secret은 앱에 넣으면 안 되므로 지우지 않는다.
    override suspend fun delete(photo: LogPhoto): Result<Unit> = Result.success(Unit)

    private fun postImage(jpeg: ByteArray): String {
        val boundary = "WalkLog-${UUID.randomUUID()}"
        val connection = URL("https://api.cloudinary.com/v1_1/$cloudName/image/upload")
            .openConnection() as HttpURLConnection
        try {
            connection.requestMethod = "POST"
            connection.doOutput = true
            connection.connectTimeout = TIMEOUT_MS
            connection.readTimeout = TIMEOUT_MS
            connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=$boundary")

            DataOutputStream(connection.outputStream).use { out ->
                out.writeBytes("--$boundary\r\n")
                out.writeBytes("Content-Disposition: form-data; name=\"upload_preset\"\r\n\r\n")
                out.write(uploadPreset.toByteArray(Charsets.UTF_8))
                out.writeBytes("\r\n")

                out.writeBytes("--$boundary\r\n")
                out.writeBytes("Content-Disposition: form-data; name=\"file\"; filename=\"photo.jpg\"\r\n")
                out.writeBytes("Content-Type: image/jpeg\r\n\r\n")
                out.write(jpeg)
                out.writeBytes("\r\n")

                out.writeBytes("--$boundary--\r\n")
            }

            val code = connection.responseCode
            val stream = if (code in 200..299) connection.inputStream else connection.errorStream
            val body = stream?.bufferedReader()?.use { it.readText() }.orEmpty()

            if (code !in 200..299) {
                val message = runCatching {
                    JSONObject(body).optJSONObject("error")?.optString("message")
                }.getOrNull()
                throw IOException("Cloudinary 업로드 실패 ($code): ${message?.takeIf { it.isNotBlank() } ?: body}")
            }

            val secureUrl = JSONObject(body).optString("secure_url")
            if (secureUrl.isBlank()) throw IOException("Cloudinary 응답에 secure_url이 없습니다: $body")
            return secureUrl
        } finally {
            connection.disconnect()
        }
    }

    private companion object {
        const val TIMEOUT_MS = 20_000
    }
}
