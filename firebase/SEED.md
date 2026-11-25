# 가짜 데이터 (에뮬레이터 시드)

`firebase/seed/`는 로컬 Firebase 에뮬레이터를 켤 때 불러오는 공용 가짜 데이터입니다. 팀원 모두 같은 사용자 · 로그 · 사진으로 화면을 만들고 확인합니다.
내용은 포트폴리오 프로토타입의 가짜 데이터를 옮긴 것입니다.

## 에뮬레이터 켜기

작업 폴더 `firebase/`에서(Windows는 PowerShell에서도 같은 명령. "스크립트를 실행할 수 없으므로" 오류가 나면 `firebase.cmd`로 부르거나 Git Bash · cmd에서 실행):

```bash
firebase emulators:start --config firebase.emulator.json --project walklog-e37fa --import ./seed
```

- 켤 때마다 이 시드 상태로 시작합니다. 앱에서 바꾼 내용은 에뮬레이터를 끄면 사라집니다.
- **`--export-on-exit`는 붙이지 마세요.** 끌 때 지금 상태가 공용 시드를 덮어써서, 다른 팀원에게까지 바뀐 데이터가 퍼집니다.

## 들어 있는 것

| 종류 | 수 | 내용 |
| --- | --- | --- |
| 사용자 (Auth · users · handles) | 7 | 석두(`seokdu.walks`), 망원 고양이(`mangwon_cat`), 혜진(`hyejin.walks`), 성수 느리게(`seongsu_slow`), 밤길 김씨(`night.kim`), 종로 단풍(`jongno.dan`), 새벽(`saebyeok_`, 로그 · 프로필 사진 없는 새 사용자) |
| 로그 (logs) | 24 | 석두 12개(나만 보기 2개 포함) + 다른 사람 12개(모두 공개). 2026-09-28 ~ 2026-11-18 |
| 사진 (Storage `photos/`) | 80 | 로그 사진 74장 + 프로필 사진 6장. 로그마다 따로 올려서, 로그를 지워도 다른 로그 사진은 남습니다 |
| 좋아요 · 저장 | 56 · 50 | 사용자끼리 실제로 누른 표시(`users/{uid}/likes`, `saves`)라 로그의 수와 맞습니다 |

- 로그 id는 `seed-l-1118`(석두), `seed-f-01`(다른 사람)처럼 고정입니다.
- 석두의 보관함에는 다른 사람 로그 11개가 최근 저장 순으로 들어 있습니다.
- 태그는 `단풍` · `야경` · `한강` · `골목` · `카페` 다섯 가지만 씁니다.
- 경로는 망원 한강공원 · 망원동 골목 · 망원시장 · 연남동 · 경의선숲길 · 성산로 · 서순라길 · 익선동 · 해방촌 · 남산 · 서울숲 · 성수 · 성북동 · 합정 강변의 실제 길을 따라 찍은 꺾임점을 5초 간격 점으로 채운 것입니다.
  - 제목 · 본문에 나오는 길은 OpenStreetMap 길 좌표를 그대로 따랐습니다(OSM way 약 90개, 100개 미만): 망원 · 합정 강변(한강북자전거길 · 성산나들목), 망원시장 길, 경의선숲길, 성산로, 서순라길 · 율곡로 · 돈화문로 일대, 해방촌 위 남산 북측순환로(남산공원길), 서울숲 산책로, 성수 연무장길, 성북동 한양도성 순성길. © OpenStreetMap contributors (https://www.openstreetmap.org/copyright)
  - 나머지(망원동 · 연남동 · 익선동 골목, 해방촌 · 소월길 등)는 손으로 찍어서(지도 매칭 안 함) 가장 가까운 OSM 길에서 최대 약 55m 벗어난 곳이 있습니다.
  - OSM 데이터로 확인한 범위: 한강 · 중랑천 · 홍제천과 서울숲 호수 · 연못 같은 물 위를 지나지 않고, 궁궐(종묘 · 창덕궁 · 창경궁 · 운현궁) 안에 들어가지 않으며, 강변북로 같은 자동차 전용 도로를 따라 걷지 않습니다.
- 거리(`distanceKm`)는 경로 점으로 계산한 값이고, 사진 위치는 찍은 시각에 걷고 있던 경로 위 지점입니다.
- 사진 주소는 `http://127.0.0.1:9199/...`로 고정입니다. `connect-device.sh`로 포트를 이은 기기에서 열리며, `local.properties`의 `EMULATOR_HOST`를 바꾸면 시드 사진이 안 보입니다.
- 탐색 피드의 최근 순은 `createdAt`(시드를 넣은 시각) 기준이라, 걸은 날짜 순서대로 넣어 두었습니다.

## 시험용 계정

에뮬레이터 안에만 있는 계정입니다. 진짜 Firebase에는 없습니다.

- 이메일: `{키}@walklog.test` — `seokdu`, `mangwon_cat`, `hyejin`, `seongsu_slow`, `nightwalker`, `jongno_dan`, `saebyeok`
- 비밀번호(모두 같음): `walklog-seed-1234`

## 다시 만들기

내용은 `android/app/src/androidTest/java/com/example/walklog/seed/`에 있습니다.

- `SeedData.kt`: 사용자 · 로그 · 사진 배치 · 좋아요 · 저장 목록
- `SeedRoute.kt`: 꺾임점을 5초 간격 경로로 채우고 거리를 계산
- `SeedFakeData.kt`: 앱의 Repository로 가입 → 프로필 → 사진 업로드 → 로그 → 좋아요 · 저장 순서로 넣고 수를 확인하는 계측 테스트
- 사진 원본: `android/app/src/androidTest/assets/seed-photos/` (Unsplash, `CREDITS.md`)

> **공용 시드는 항상 빈 에뮬레이터에서 만듭니다.** 시드 테스트는 마지막에 수 · 사진 파일이 시드와 정확히 같은지 확인해서, 직접 넣은 데이터(가입한 계정, 새 로그, 누른 좋아요 · 저장, 올린 사진)가 섞여 있으면 실패합니다.

Windows 안내: Git으로 받으면 `gradlew` · `connect-device.sh`의 줄바꿈이 CRLF로 바뀌어 Git Bash에서도 `./gradlew`, `.sh`가 실패할 수 있습니다. 아래 각 단계의 Windows 명령(PowerShell 또는 Android Studio 터미널)을 쓰세요.

순서:

1. 켜 둔 에뮬레이터가 있으면 먼저 끕니다(그 터미널에서 Ctrl+C, 포트가 겹칩니다). 그다음 **빈 에뮬레이터**를 켭니다(`--import` 없이). 작업 폴더: `firebase/`

   ```bash
   firebase emulators:start --config firebase.emulator.json --project walklog-e37fa
   ```

2. 1번 터미널은 에뮬레이터가 계속 쓰므로, **새 터미널을 열어 레포 루트에서** 진행합니다. `android/local.properties`가 `USE_EMULATOR=true`인지 확인하고 기기를 켭니다.

   ```bash
   ./firebase/connect-device.sh
   adb shell pm clear com.example.walklog
   ```

   Windows(PowerShell)는 스크립트 대신 포트를 직접 잇습니다.

   ```powershell
   $adb = "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe"
   & $adb reverse tcp:9099 tcp:9099
   & $adb reverse tcp:8080 tcp:8080
   & $adb reverse tcp:9199 tcp:9199
   & $adb shell pm clear com.example.walklog
   ```

   - `pm clear`는 앱을 쓰던 기기에 남은 예전 데이터(로컬 캐시 · 로그인)를 지워 깨끗한 상태로 시작하게 합니다. 앱이 설치돼 있지 않으면 오류가 나도 괜찮습니다.
   - macOS에서 `adb`를 못 찾으면 SDK의 `platform-tools` 폴더를 PATH에 넣거나 `~/Library/Android/sdk/platform-tools/adb`로 부릅니다.
   - **기기는 하나만** 연결하거나 `ANDROID_SERIAL`로 하나를 지정하세요. 시드 테스트는 연결된 기기마다 돌아갑니다.

3. 시드 테스트만 실행합니다(`seed=true`가 없으면 건너뜁니다). 작업 폴더: `android/`

   ```bash
   ./gradlew connectedDebugAndroidTest \
     -Pandroid.testInstrumentationRunnerArguments.class=com.example.walklog.seed.SeedFakeData \
     -Pandroid.testInstrumentationRunnerArguments.seed=true
   ```

   Windows(PowerShell · Android Studio 터미널)는 한 줄로, `-P` 인자를 따옴표로 감쌉니다(따옴표가 없으면 PowerShell이 `.`에서 인자를 잘라 버립니다).

   ```powershell
   .\gradlew.bat connectedDebugAndroidTest "-Pandroid.testInstrumentationRunnerArguments.class=com.example.walklog.seed.SeedFakeData" "-Pandroid.testInstrumentationRunnerArguments.seed=true"
   ```

   - 테스트가 끝나면 Gradle이 앱을 지워서, 기기의 앱 데이터와 로그인도 사라집니다. 앱은 다시 실행하면 됩니다.

4. 에뮬레이터를 켠 채로 내보냅니다. 작업 폴더: `firebase/` (`--force`는 이미 있는 `seed/`를 묻지 않고 덮어씁니다). **`--config firebase.emulator.json`을 꼭 붙이세요.** 빠지면 Auth 계정이 빠진 채로 내보내집니다.

   ```bash
   firebase emulators:export ./seed --config firebase.emulator.json --project walklog-e37fa --force
   ```

5. `seed/`의 바뀐 파일을 커밋합니다. 내보내기는 `seed/` 폴더를 통째로 지우고 다시 만들어서, 이 안내 문서는 `seed/` 밖(`firebase/SEED.md`)에 둡니다.

참고:

- 다시 돌려도 되는 경우는 에뮬레이터에 **시드만** 들어 있을 때(예: `--import ./seed`로 막 켠 직후)뿐입니다. 이때는 있는 계정 · 프로필 · 로그를 건너뛰고 좋아요 · 저장도 두 번 더하지 않으니, 시드가 멀쩡한지 확인하는 데 씁니다. 그 밖에는 언제나 빈 에뮬레이터에서 다시 만듭니다.
- 지금은 `USE_EMULATOR=true`일 때만 돌아갑니다. 시연용 진짜 Firebase에 넣으려면 `SeedFakeData.kt`에서 세 군데를 일부러 풀어야 합니다: `check(BuildConfig.USE_EMULATOR)`, 사진 주소가 `http://127.0.0.1:9199/`로 시작하는지 보는 확인(`EMULATOR_PHOTO_PREFIX`), 에뮬레이터 Storage 파일을 세는 `verifyNoOrphanPhotos`. 사진은 Cloudinary로 올라가서 주소가 다르고, 진짜 Firebase Storage는 쓰지 않기 때문입니다.
  **이때 `SeedData.PASSWORD`를 반드시 다른 값으로 바꾸세요.** 공개 레포에 적힌 비밀번호라, 그대로 넣으면 누구나 시드 계정으로 로그인할 수 있습니다. 바꾼 값은 커밋하지 않습니다.
- 시드 크기는 약 24MB입니다(대부분 사진). 다시 만들 때마다 git 기록에 그만큼 쌓이니 꼭 필요할 때만 다시 만듭니다.
