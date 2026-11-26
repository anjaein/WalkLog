package com.example.walklog.seed

import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.walklog.BuildConfig
import com.example.walklog.WalkLogApplication
import com.example.walklog.data.AppContainer
import com.example.walklog.data.model.LogPhoto
import com.example.walklog.data.model.User
import com.example.walklog.data.model.Visibility
import com.example.walklog.data.model.WalkLog
import com.google.firebase.Firebase
import com.example.walklog.data.firebase.FirestorePaths
import com.google.firebase.firestore.Source
import com.google.firebase.firestore.firestore
import com.google.firebase.storage.storage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/**
 * 가짜 데이터를 앱의 Repository 로 넣는다. 평소 테스트에 섞여 돌지 않게 seed=true 인자를 줄 때만 실행된다.
 * 쓰는 법과 내보내기(export)는 firebase/SEED.md.
 *
 * ./gradlew connectedDebugAndroidTest \
 *   -Pandroid.testInstrumentationRunnerArguments.class=com.example.walklog.seed.SeedFakeData \
 *   -Pandroid.testInstrumentationRunnerArguments.seed=true
 */
@RunWith(AndroidJUnit4::class)
class SeedFakeData {

    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val container: AppContainer
        get() = (instrumentation.targetContext.applicationContext as WalkLogApplication).container
    private val uids = mutableMapOf<String, String>()
    private val copiedPhotos = mutableSetOf<String>()

    @Test
    fun seed() = runBlocking {
        assumeTrue("seed=true 인자를 줄 때만 넣는다", InstrumentationRegistry.getArguments().getString("seed") == "true")
        // 지금은 에뮬레이터에만 넣는다. 진짜 Firebase · Cloudinary 에 넣으려면 이 확인을 일부러 풀어야 하고,
        // verify() 의 사진 주소 확인(EMULATOR_PHOTO_PREFIX)과 verifyNoOrphanPhotos(에뮬레이터 Storage 를 센다)도 함께 풀어야 한다.
        // Cloudinary 주소는 다르고, 진짜 Firebase Storage 는 쓰지 않는다
        check(BuildConfig.USE_EMULATOR) { "USE_EMULATOR=true 일 때만 가짜 데이터를 넣는다" }
        clearLocalCache()

        SeedData.users.forEach { ensureUser(it) }
        // 탐색 피드의 최근 순은 createdAt(넣은 시각)으로 정렬돼서, 걸은 날짜 순으로 넣어야 순서가 맞는다
        SeedData.logs.sortedBy { startedAt(it) }.forEach { ensureLog(it) }
        SeedData.users.forEach { addLikesAndSaves(it) }
        verify()
        container.authRepository.signOut()
    }

    // 시드 id 는 고정이라, 앱을 쓰던 기기에는 예전 시드 로그가 로컬 캐시에 남아 있다.
    // 그 값을 서버 값으로 믿으면 빈 에뮬레이터에서 다시 만들 때 로그를 건너뛰므로, 시작 전에 캐시를 비운다
    private suspend fun clearLocalCache() {
        val cleared = runCatching { Firebase.firestore.clearPersistence().await() }
        check(cleared.isSuccess) {
            "Firestore 로컬 캐시를 비우지 못했다. class 필터로 SeedFakeData 만 실행하고(다른 테스트가 먼저 Firestore 를 쓰면 실패한다), " +
                "그래도 안 되면 adb shell pm clear com.example.walklog 로 앱 데이터를 지우고 다시 실행 (${cleared.exceptionOrNull()})"
        }
    }

    private suspend fun ensureUser(seed: SeedUser) {
        val auth = container.authRepository
        auth.signOut()
        // 처음이면 가입하고, 다시 돌릴 때는 이미 있는 계정으로 로그인한다
        val signUp = auth.signUpWithEmail(seed.email, SeedData.PASSWORD)
        val uid = signUp.getOrNull() ?: auth.signInWithEmail(seed.email, SeedData.PASSWORD).getOrElse { signInError ->
            throw IllegalStateException("${seed.email}: 가입도 로그인도 안 된다. 가입 실패 원인: ${signUp.exceptionOrNull()}", signInError)
        }
        uids[seed.key] = uid
        if (existsOnServer("${FirestorePaths.USERS}/$uid")) return
        val photoUrl = seed.avatar?.let { upload(it).url }
        val user = User(
            uid = uid,
            name = seed.name,
            handle = seed.handle,
            photoUrl = photoUrl,
            bio = seed.bio,
            area = seed.area,
            headerColor = seed.headerColor,
            defaultVisibility = Visibility.PUBLIC,
            language = "ko",
            joinedAt = SeedRoute.epochMillis(seed.joined, "12:00"),
        )
        container.userRepository.createProfile(user).getOrThrow()
    }

    private suspend fun ensureLog(seed: SeedLog) {
        val uid = signInAs(seed.author)
        if (existsOnServer("${FirestorePaths.LOGS}/${seed.id}")) return
        val startedAt = startedAt(seed)
        val endedAt = SeedRoute.epochMillis(seed.date, seed.end)
        val route = SeedRoute(seed.waypoints, startedAt, endedAt, seed.id.hashCode())
        val points = route.points()
        // 가공한 사진에는 EXIF 가 없어서, 위치는 찍은 시각에 걷고 있던 경로 위 지점으로 채운다
        val photos = seed.photos.map { photo ->
            val takenAt = SeedRoute.epochMillis(seed.date, photo.time)
            val (lat, lng) = route.positionAt(takenAt)
            upload(photo.file).copy(lat = lat, lng = lng, takenAt = takenAt, note = photo.note)
        }
        val log = WalkLog(
            id = seed.id,
            authorId = uid,
            title = seed.title,
            text = seed.text,
            startedAt = startedAt,
            endedAt = endedAt,
            minutes = ((endedAt - startedAt) / 60_000).toInt(),
            distanceKm = SeedRoute.distanceKm(points),
            place = seed.place,
            visibility = seed.visibility,
            tags = seed.tags,
            route = points,
            photos = photos,
        )
        container.logRepository.createLog(log).getOrThrow()
    }

    private suspend fun addLikesAndSaves(seed: SeedUser) {
        val liked = SeedData.logs.filter { seed.key in it.likedBy }
        val saved = SeedData.logs.filter { seed.key in it.savedBy }
        if (liked.isEmpty() && saved.isEmpty()) return
        val uid = signInAs(seed.key)
        liked.forEach { container.likeRepository.setLiked(uid, it.id, true).getOrThrow() }
        // 보관함은 최근 저장 순이라, 보관함 목록의 뒤(오래된 것)부터 저장한다
        val archiveOrder = SeedData.seokduArchive.reversed()
        saved.sortedBy { archiveOrder.indexOf(it.id) }
            .forEach { container.saveRepository.setSaved(uid, it.id, true).getOrThrow() }
    }

    private suspend fun verify() {
        val photoUrls = mutableListOf<String>()
        SeedData.users.forEach { seed ->
            val uid = signInAs(seed.key)
            val user = container.userRepository.observeUser(uid).awaitValue(VERIFY_TIMEOUT_MS)
            assertNotNull("${seed.key}: 프로필이 없다", user)
            val profile = "${seed.key}: 저장된 프로필이 SeedData 와 다르다. SeedData 를 고쳤다면 빈 에뮬레이터에서 다시 만든다"
            assertEquals(profile, seed.handle, user!!.handle)
            assertEquals(profile, seed.name, user.name)
            assertEquals(profile, seed.bio, user.bio)
            assertEquals(profile, seed.area, user.area)
            assertEquals(profile, seed.headerColor, user.headerColor)
            assertEquals(profile, SeedRoute.epochMillis(seed.joined, "12:00"), user.joinedAt)
            assertEquals("${seed.key}: 프로필 사진", seed.avatar != null, user.photoUrl != null)
            user.photoUrl?.let { photoUrls += it }
        }
        SeedData.users.forEach { seed ->
            val handle = Firebase.firestore.document("${FirestorePaths.HANDLES}/${seed.handle}").get(Source.SERVER).await()
            assertEquals("${seed.handle}: handles 문서의 uid 가 ${seed.key} 가 아니다", uids.getValue(seed.key), handle.getString("uid"))
        }
        SeedData.logs.forEach { seed ->
            signInAs(seed.author)
            val log = withTimeoutOrNull(VERIFY_TIMEOUT_MS) {
                container.logRepository.observeLog(seed.id).filterNotNull().first {
                    it.likeCount == seed.likedBy.size && it.saveCount == seed.savedBy.size && it.photos.size == seed.photos.size
                }
            }
            assertNotNull("${seed.id}: 로그가 없거나 좋아요 · 저장 · 사진 수가 다르다. 기기에 예전 앱 데이터가 남아 있었다면 adb shell pm clear com.example.walklog 로 지우고 빈 에뮬레이터에서 다시 실행", log)
            assertTrue(seed.id, log!!.route.size > 100)
            // 불러온 시드가 지금 SeedData 와 같은지 본다. SeedData 를 고친 뒤 예전 시드 위에서 돌리면 여기서 걸린다
            val endedAt = SeedRoute.epochMillis(seed.date, seed.end)
            val expected = SeedRoute(seed.waypoints, startedAt(seed), endedAt, seed.id.hashCode())
            val expectedRoute = expected.points()
            val stale = "${seed.id}: 저장된 로그가 SeedData 와 다르다. SeedData 를 고쳤다면 빈 에뮬레이터에서 다시 만든다"
            assertEquals(stale, seed.title, log.title)
            assertEquals(stale, seed.text, log.text)
            assertEquals(stale, seed.place, log.place)
            assertEquals(stale, seed.tags, log.tags)
            assertEquals(stale, seed.visibility, log.visibility)
            assertEquals(stale, startedAt(seed), log.startedAt)
            assertEquals(stale, endedAt, log.endedAt)
            assertEquals(stale, SeedRoute.distanceKm(expectedRoute), log.distanceKm, 0.0)
            assertEquals(stale, expectedRoute, log.route)
            seed.photos.zip(log.photos).forEach { (seedPhoto, photo) ->
                val takenAt = SeedRoute.epochMillis(seed.date, seedPhoto.time)
                val (lat, lng) = expected.positionAt(takenAt)
                assertEquals(stale, takenAt, photo.takenAt)
                assertEquals(stale, seedPhoto.note, photo.note)
                assertEquals(stale, lat, photo.lat!!, 0.0)
                assertEquals(stale, lng, photo.lng!!, 0.0)
            }
            photoUrls += log.photos.map { it.url }
        }
        // 사진 주소는 에뮬레이터 주소가 그대로 저장된다. EMULATOR_HOST 를 바꾼 채 만들면 다른 팀원 기기에서 사진이 안 열린다
        photoUrls.forEach { url ->
            assertTrue("사진 주소가 $EMULATOR_PHOTO_PREFIX 로 시작하지 않는다(EMULATOR_HOST 확인): $url", url.startsWith(EMULATOR_PHOTO_PREFIX))
        }
        val expectedPhotos = SeedData.logs.sumOf { it.photos.size } + SeedData.users.count { it.avatar != null }
        assertEquals("사진 주소가 겹친다", expectedPhotos, photoUrls.toSet().size)
        verifyNoOrphanPhotos(photoUrls.toSet())

        val seokdu = signInAs("seokdu")
        val archive = withTimeoutOrNull(VERIFY_TIMEOUT_MS) {
            container.saveRepository.observeSavedLogIds(seokdu).first { it.size == SeedData.seokduArchive.size }
        }
        assertNotNull("석두 보관함이 ${SeedData.seokduArchive.size}개가 아니다", archive)
        assertEquals("석두 보관함 순서", SeedData.seokduArchive, archive)
    }

    // 존재 확인이 시간 안에 서버 값을 못 받아 사진을 다시 올리면, 어디에도 안 쓰이는 파일이 Storage 에 남는다.
    // PhotoRepository 에는 목록 기능이 없어서 Storage SDK 로 직접 센다
    private suspend fun verifyNoOrphanPhotos(referenced: Set<String>) {
        val stored = Firebase.storage.reference.child("photos").listAll().await().items.map { it.name }.toSet()
        val used = referenced.mapNotNull { Uri.parse(it).lastPathSegment?.substringAfter("photos/") }.toSet()
        assertEquals("Storage photos/ 에 로그 · 프로필이 안 쓰는 사진이 있다. 빈 에뮬레이터에서 다시 만든다", used, stored)
    }

    // 이미 넣었는지는 캐시가 아니라 서버에 묻는다. Repository 의 observe 는 캐시 값이 먼저 올 수 있어 '없음'과 '아직 모름'을 못 가린다
    private suspend fun existsOnServer(path: String): Boolean {
        val result = runCatching { Firebase.firestore.document(path).get(Source.SERVER).await() }
        val snapshot = checkNotNull(result.getOrNull()) {
            "$path 를 서버에서 읽지 못했다. Firebase 에뮬레이터가 켜져 있고 connect-device.sh 로 포트를 이었는지 확인 (${result.exceptionOrNull()})"
        }
        return snapshot.exists()
    }

    // 트랜잭션으로 쓴 문서는 로컬 캐시에 바로 들어오지 않아서, 첫 값이 캐시의 '없음'일 수 있다. 서버 값이 올 때까지 기다린다
    private suspend fun <T : Any> Flow<T?>.awaitValue(timeoutMs: Long): T? =
        withTimeoutOrNull(timeoutMs) { filterNotNull().first() }

    private suspend fun signInAs(key: String): String {
        val auth = container.authRepository
        val uid = uids.getValue(key)
        if (auth.currentUserIdNow() == uid) return uid
        auth.signOut()
        val email = SeedData.users.first { it.key == key }.email
        return auth.signInWithEmail(email, SeedData.PASSWORD).getOrThrow()
    }

    private suspend fun upload(file: String): LogPhoto {
        val uri = Uri.fromFile(photoFile(file))
        return container.photoRepository.upload(uri).getOrThrow()
    }

    // 테스트 APK 의 assets 는 앱이 열 수 없어서, 앱 캐시 폴더로 한 번 복사해 두고 그 파일 주소로 올린다
    // 지난 실행이 복사 도중 끊겼으면 잘린 파일이 남아 있을 수 있어서, 실행마다 처음 쓸 때 새로 복사한다.
    // 임시 파일에 다 쓴 뒤 이름을 바꿔, 잘린 파일이 제 이름으로 남지 않게 한다
    private fun photoFile(file: String): File {
        val target = File(instrumentation.targetContext.cacheDir, "seed-photos/$file.jpg")
        if (file in copiedPhotos) return target
        target.parentFile?.mkdirs()
        val temp = File(target.parentFile, "$file.jpg.tmp")
        instrumentation.context.assets.open("seed-photos/$file.jpg").use { input ->
            temp.outputStream().use { input.copyTo(it) }
        }
        check(temp.renameTo(target)) { "시드 사진을 제자리로 옮기지 못했다: $target" }
        copiedPhotos += file
        return target
    }

    private fun startedAt(seed: SeedLog): Long = SeedRoute.epochMillis(seed.date, seed.start)

    private companion object {
        const val VERIFY_TIMEOUT_MS = 15_000L
        const val EMULATOR_PHOTO_PREFIX = "http://127.0.0.1:9199/"
    }
}
