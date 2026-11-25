package com.example.walklog

import android.app.Application
import com.example.walklog.data.firebase.FirebaseEmulator
import com.kakao.vectormap.KakaoMapSdk

class WalkLogApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // 에뮬레이터 연결은 Firebase를 처음 쓰기 전에 해야 해서 앱 시작 직후에 둔다
        FirebaseEmulator.connectIfEnabled()
        if (BuildConfig.KAKAO_NATIVE_APP_KEY.isNotBlank()) {
            KakaoMapSdk.init(this, BuildConfig.KAKAO_NATIVE_APP_KEY)
        }
    }
}
