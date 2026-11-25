package com.example.walklog.data.firebase

import com.example.walklog.BuildConfig
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.firestore
import com.google.firebase.storage.storage

/** local.properties의 USE_EMULATOR=true 면 진짜 Firebase 대신 내 노트북의 로컬 에뮬레이터에 붙는다. */
object FirebaseEmulator {
    // 레포 firebase/firebase.json 의 포트와 같아야 한다
    private const val AUTH_PORT = 9099
    private const val FIRESTORE_PORT = 8080
    private const val STORAGE_PORT = 9199

    fun connectIfEnabled() {
        if (!BuildConfig.USE_EMULATOR) return
        val host = BuildConfig.EMULATOR_HOST
        Firebase.auth.useEmulator(host, AUTH_PORT)
        Firebase.firestore.useEmulator(host, FIRESTORE_PORT)
        Firebase.storage.useEmulator(host, STORAGE_PORT)
    }
}
