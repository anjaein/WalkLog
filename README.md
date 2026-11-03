# Walklog

> Record your walk. Pin your photos. Share the way.

GPS로 산책 경로를 기록하고, 찍은 사진을 지도 위 촬영 위치에 핀으로 꽂아 하나의 코스로 공유하는 Android 앱입니다.

**Team 뫱뿌볼** · 안재인 · 인지환 · 황대경

## Tech Stack

- **App**: Kotlin, Jetpack Compose
- **Map / Location**: Google Maps SDK, Fused Location Provider
- **Camera**: CameraX
- **Backend**: Firebase Auth, Cloud Firestore, Cloud Storage

## 폴더 구조

```
WalkLog/
├── android/                 # Android Studio 프로젝트 (이 폴더를 Android Studio로 열기)
│   ├── app/
│   ├── gradle/
│   ├── build.gradle.kts
│   └── settings.gradle.kts
├── firebase/                # 백엔드 설정 · 보안 규칙
├── docs/                    # 기획안, 화면 설계, DB 스키마 메모
├── .github/                 # 이슈 · PR 템플릿
├── .coderabbit.yaml         # CodeRabbit 리뷰 설정
├── AGENTS.md                # AI 작업 규칙 (CLAUDE.md가 참조)
├── CONVENTION.md            # 협업 컨벤션
└── README.md
```

### 앱 패키지 구조 (`android/app/src/main/java/.../walklog/`)

```
walklog/
├── feature/
│   ├── auth/        # 로그인 · 회원가입
│   ├── record/      # GPS 기록, 산책 로그 CRUD
│   ├── photomap/    # 사진 지도, 경로 위 사진 핀
│   ├── discover/    # 코스 검색 · 테마 추천
│   └── mypage/      # 마이페이지 · 프로필
├── data/            # 외부 서비스 접근 (repository)
├── domain/          # GPS 필터링, 거리 · 구간 계산, 사진 ↔ 경로 매칭, 코스 빌더
└── ui/
    ├── theme/
    └── components/  # 공통 Compose 컴포넌트
```

## ▶️ 실행 방법

1. 레포 clone

   ```bash
   git clone https://github.com/anjaein/WalkLog.git
   ```

2. Android Studio에서 **`android/` 폴더**를 열기 (레포 루트 X)
3. Gradle Sync가 끝날 때까지 대기
4. 상단 기기 목록에서 에뮬레이터 또는 연결한 테스트 폰 선택
5. ▶️ Run `app`

> `android/local.properties`는 Android Studio가 SDK 경로로 자동 생성합니다. 커밋하지 마세요.

## 📚 문서

| 문서 | 내용 |
| ---- | ---- |
| [Convention](./CONVENTION.md) | 커밋 · 브랜치 · 네이밍 컨벤션과 라벨링 필수 규칙을 정리했습니다. |

## 역할

| 이름 | 담당 |
| --- | --- |
| 인지환 | 로그인 · 사용자 인증, 앱 테스트 |
| 안재인 | 사진 지도 · 경로 핀 표시, 코스 검색 · 추천 |
| 황대경 | 마이페이지 · 프로필 관리, 최종 발표 |
