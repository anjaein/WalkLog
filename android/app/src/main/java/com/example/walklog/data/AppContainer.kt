package com.example.walklog.data

import android.content.Context
import com.example.walklog.BuildConfig
import com.example.walklog.data.auth.AuthRepository
import com.example.walklog.data.auth.FirebaseAuthRepository
import com.example.walklog.data.like.FirebaseLikeRepository
import com.example.walklog.data.like.LikeRepository
import com.example.walklog.data.location.FusedLocationRepository
import com.example.walklog.data.location.LocationRepository
import com.example.walklog.data.log.FirebaseLogRepository
import com.example.walklog.data.log.LogRepository
import com.example.walklog.data.photo.CloudinaryPhotoRepository
import com.example.walklog.data.photo.PhotoProcessor
import com.example.walklog.data.photo.PhotoRepository
import com.example.walklog.data.photo.StoragePhotoRepository
import com.example.walklog.data.save.FirebaseSaveRepository
import com.example.walklog.data.save.SaveRepository
import com.example.walklog.data.user.FirebaseUserRepository
import com.example.walklog.data.user.UserRepository
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.firestore
import com.google.firebase.storage.storage

/**
 * 앱 전체가 같이 쓰는 Repository 모음. 앱 시작 때 한 번 만들고, ViewModel은 여기서 꺼내 쓴다.
 * 화면 코드는 어떤 구현(Firebase · Cloudinary · 에뮬레이터)이 붙는지 몰라도 된다.
 */
interface AppContainer {
    val authRepository: AuthRepository
    val userRepository: UserRepository
    val logRepository: LogRepository
    val photoRepository: PhotoRepository
    val likeRepository: LikeRepository
    val saveRepository: SaveRepository
    val locationRepository: LocationRepository
}

class DefaultAppContainer(context: Context) : AppContainer {
    private val appContext = context.applicationContext

    override val authRepository: AuthRepository by lazy { FirebaseAuthRepository(Firebase.auth) }
    override val userRepository: UserRepository by lazy { FirebaseUserRepository(Firebase.firestore) { authRepository.currentUserIdNow() } }
    override val logRepository: LogRepository by lazy {
        FirebaseLogRepository(Firebase.firestore, photoRepository) { authRepository.currentUserIdNow() }
    }
    override val likeRepository: LikeRepository by lazy { FirebaseLikeRepository(Firebase.firestore) { authRepository.currentUserIdNow() } }
    override val saveRepository: SaveRepository by lazy { FirebaseSaveRepository(Firebase.firestore) { authRepository.currentUserIdNow() } }
    override val locationRepository: LocationRepository by lazy { FusedLocationRepository(appContext) }

    // 진짜 Firebase Storage는 카드 등록이 필요해서, 평소(에뮬레이터)엔 에뮬레이터 Storage, 시연 땐 Cloudinary에 올린다
    override val photoRepository: PhotoRepository by lazy {
        val processor = PhotoProcessor(appContext.contentResolver)
        if (BuildConfig.USE_EMULATOR) {
            StoragePhotoRepository(processor, Firebase.storage)
        } else {
            CloudinaryPhotoRepository(processor, BuildConfig.CLOUDINARY_CLOUD_NAME, BuildConfig.CLOUDINARY_UPLOAD_PRESET)
        }
    }
}
