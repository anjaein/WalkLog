# Walklog AI 규칙

> 팀 공용 파일이다. AI 도구(Claude Code, Codex 등)로 작업할 때 이 규칙을 따른다.
> Claude Code는 `CLAUDE.md`(`@AGENTS.md`)를 통해 이 파일을 읽는다.

## 프로젝트 맥락

- GPS로 산책 경로를 기록하고, 사진을 촬영 위치에 핀으로 꽂아 코스로 공유하는 Android 앱이다.
- Kotlin + Jetpack Compose. Android 프로젝트 루트는 레포 루트가 아니라 `android/`다.
- 의존성 버전은 `android/gradle/libs.versions.toml`(Version Catalog)에서 관리한다.
- 팀 협업 규칙(커밋 · 브랜치 · 네이밍)은 `CONVENTION.md`를 따른다. 특히 상단의 **라벨링 필수 규칙**(코드 위치, PR 변경 유형 체크)은 반드시 지킨다.

## 폴더 역할

패키지 루트: `android/app/src/main/java/com/example/walklog/`

- `feature/auth`: 로그인 · 사용자 인증
- `feature/record`: GPS 기록, 산책 로그 CRUD
- `feature/photomap`: 사진 지도, 경로 위 사진 핀
- `feature/discover`: 코스 검색 · 테마 추천
- `feature/mypage`: 마이페이지 · 프로필
- `data`: 외부 서비스(백엔드 · 지도 · 위치 · 저장소) 접근(Repository)
- `domain`: GPS 필터링, 거리 · 구간 계산, 사진 ↔ 경로 매칭, 코스 빌더
- `ui/theme`: 디자인 토큰(`Color.kt`, `Type.kt`, `Shape.kt`, `Spacing.kt`)과 앱 테마
- `ui/components`: 공통 Compose 컴포넌트
- `firebase/`(레포 루트): 백엔드 보안 규칙 · 설정

PR 라벨러가 파일 경로로 영역 라벨을 붙이므로, 새 코드는 반드시 위 폴더 중 맞는 곳에 둔다. 폴더 이름을 바꾸거나 새 최상위 폴더를 만들지 않는다.

## 코드 작성 방식

- 새로 만들기 전에 `ui/components`, `ui/theme`, `domain`, `data`에 재사용 가능한 것이 있는지 먼저 확인한다.
- 화면은 `XxxScreen`(Composable) + `XxxViewModel` + `XxxUiState` 구조로 만든다.
- 화면 · ViewModel에서 외부 SDK(백엔드, 지도, 위치, 카메라 등)를 직접 호출하지 않는다. `data`의 Repository 인터페이스 뒤에 숨겨서, SDK를 바꿔도 화면 코드가 바뀌지 않게 한다.
- GPS 필터링 · 거리 계산 같은 순수 로직은 Android · 외부 SDK 의존성 없이 `domain`에 두고 단위 테스트를 붙인다.
- 새 라이브러리를 추가해야 하면 먼저 사용자에게 이유와 대안을 설명하고 확인받는다.
- 색상 · 글꼴 · 간격 · 모양은 `ui/theme`의 토큰을 사용한다. 토큰에 없는 값(`Color(0xFF...)`, 직접 쓴 `dp` · `sp` 등)은 시안 작업 중엔 써도 되지만, 푸시 전에는 토큰으로 옮겨야 한다.
- 명시적인 리디자인 요청이 없으면 기존 색상 · 간격 · 컴포넌트 패턴을 따른다.
- 화면에 보이는 문구는 한국어로 작성하고 `res/values/strings.xml`에 둔다.
- API 키 · 비밀 값은 `local.properties`에 두고 코드나 커밋에 넣지 않는다.

## 주석

- 코드를 읽어서 알 수 있는 내용(무엇을 하는지)에는 주석을 달지 않는다.
- 코드만으로 이유를 알기 어려운 곳에만 짧게 단다.
  - 예: GPS 튐 보정 기준값, 권한 요청 순서 의존성, 기기 · OS 버전 대응, 의도적인 우회 코드
- 대신 작업이 끝나면 무엇을 왜 바꿨는지 답변으로 설명한다.

## Git 안전 규칙

- 코드 변경 전 현재 브랜치와 `git status`를 확인한다.
- 작업 브랜치는 `develop`에서 분기한다. 현재 브랜치가 `develop`에서 분기하지 않았거나 `develop` · `main` 자체라면 작업 전에 알린다.
- 명시적으로 요청받지 않는 한 commit, push, reset, rebase, merge, 파일 삭제를 하지 않는다.
- 커밋 · 브랜치 이름은 `CONVENTION.md`를 따른다. 사용자가 다른 형식을 원하면 그대로 진행하되, 무엇이 다른지 먼저 한 줄로 알린다.
- 커밋에 `Co-Authored-By` 같은 AI 공동 작성자 줄을 넣지 않는다.
- 푸시 전에 변경분에 `ui/theme` 토큰이 아닌 디자인 값이 있는지 확인한다. 있으면 **푸시는 하지 않는다.** 해당 파일과 값을 알려주고, 의도된 작업인지 물어본 뒤 직접 푸시를 안내한다.
- 작업에 꼭 필요하지 않다면 `*.gradle.kts`, `libs.versions.toml`, `gradle.properties`, `AndroidManifest.xml`, `.gitignore`, `.github/`, `firebase/` 같은 공유 설정 파일을 수정하지 않는다.
- `local.properties`, 비밀 키, `build/` 결과물은 커밋하지 않는다.
- PR은 `develop` 대상으로 만들고, 템플릿의 **변경 유형** 줄은 `[x]` 체크만 한다. merge는 하지 않는다.

## 검증

코드 변경 후 `android/` 폴더에서 가능한 범위로 아래를 실행한다.

- `./gradlew assembleDebug`: 빌드
- `./gradlew testDebugUnitTest`: 단위 테스트 (관련 테스트가 있거나 로직을 바꾼 경우)
- `./gradlew lintDebug`: Android Lint

실행하지 못했거나 실패한 검증이 있으면 결과와 함께 명확히 알린다.

## 소통 방식

- 기본적으로 한국어로 답변한다.
- 파일을 수정하기 전에 무엇을 왜 바꿀지 짧게 설명한다.
- 팀에 영향을 줄 수 있는 애매한 변경은 공유 파일을 건드리기 전에 먼저 물어본다.
