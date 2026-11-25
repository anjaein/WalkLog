# 🛠️ 개발 환경 세팅

새 노트북에서 WalkLog를 처음 돌리는 팀원을 위한 안내서입니다. 순서대로 따라 하면 내 노트북에서 Firebase 에뮬레이터(Auth · Firestore · Storage)가 돌아가고, Android 기기에서 앱이 그 Firebase 에뮬레이터를 바라보고 실행됩니다.

> 지금 앱 화면은 모두 자리표시자라서, 앱을 눌러 봐도 Firebase에 데이터가 생기지 않습니다.

세팅이 끝났는지는 이렇게 확인합니다.

1. http://127.0.0.1:4000 (Firebase Emulator UI)이 열린다.
2. 포트 연결 결과(`adb reverse --list`)에 9099 · 8080 · 9199 세 포트가 보인다.
3. 앱이 실행되어 첫 화면(홈 자리표시자)이 뜬다.

진짜 Firebase는 맞춰 보기 · 시연 때만 씁니다([3. 진짜 Firebase로 맞춰 보기 · 시연](#3-진짜-firebase로-맞춰-보기--시연)).

이 문서에서 "에뮬레이터"는 두 가지입니다.

- **Android 에뮬레이터(가상 기기)**: 노트북 안에서 도는 가짜 폰. 테스트 폰 대신 씁니다.
- **Firebase 에뮬레이터**: 노트북에서 도는 가짜 Firebase(Auth · Firestore · Storage). 평소 개발은 여기에 붙습니다.

## 목차

- [0. 준비물](#0-준비물)
- [1. 처음 한 번](#1-처음-한-번)
- [2. 개발할 때마다](#2-개발할-때마다)
- [3. 진짜 Firebase로 맞춰 보기 · 시연](#3-진짜-firebase로-맞춰-보기--시연)
- [4. 자주 막히는 문제](#4-자주-막히는-문제)

> **macOS / Windows**
> - 명령은 macOS 터미널 기준입니다. Windows에서 다르게 해야 하는 곳은 **Windows** 표시를 달았습니다.
> - Windows에서 `./gradlew` · `connect-device.sh`는 Git Bash에서도 실패할 수 있습니다. 대신 쓸 방법은 [자주 막히는 문제](#windows-git-bash에서-gradlew나-connect-device-스크립트가-실패함)에 있습니다.

## 0. 준비물

| 도구 | 버전 | 확인 명령 |
| --- | --- | --- |
| Android Studio | 최신 | - |
| Node.js | LTS | `node -v` |
| Java | 21 이상 (Firebase 에뮬레이터용) | `java -version` |
| Firebase CLI | 14 이상 | `firebase --version` |
| Git | - (Windows는 Git for Windows) | `git --version` |

- Java 21 설치
  - macOS: `brew install --cask temurin@21`
  - **Windows**: Adoptium에서 Temurin 21 MSI를 받아 설치합니다. 설치 화면에서 PATH 추가와 `JAVA_HOME` 설정 항목을 켜 두세요.
- 앱을 빌드하는 JDK(25)는 Gradle이 첫 Sync 때 알아서 내려받습니다(인터넷 필요). 위의 Java 21은 Firebase 에뮬레이터를 돌리는 용도입니다.
- 팀 드라이브 "2026-모바일앱개발-팀플" 폴더에 들어갈 수 있어야 합니다. `google-services.json`과 "local.properties 값.txt"가 여기 있습니다. 권한이 없으면 안재인에게 요청하세요.

## 1. 처음 한 번

1. 준비물 설치. Firebase CLI는 Node.js를 깐 뒤 설치합니다.

   ```bash
   npm install -g firebase-tools
   firebase --version   # 14 이상인지 확인
   java -version        # 21 이상인지 확인
   ```

   > **Windows**: PowerShell에서 스크립트를 실행할 수 없다는 오류(running scripts is disabled)가 나면 [자주 막히는 문제](#powershell에서-npm--firebase가-스크립트-실행-오류로-안-됨)를 보세요.

2. 레포 clone

   ```bash
   git clone https://github.com/anjaein/WalkLog.git
   ```

3. Android Studio에서 **`android/` 폴더**를 엽니다(레포 루트가 아닙니다). Gradle Sync가 끝날 때까지 기다리세요.
   - Sync 중에 Android SDK Platform 37(compileSdk)을 설치하라거나 라이선스에 동의하라는 안내가 뜨면 설치를 누릅니다.
   - 이때 `android/local.properties`가 SDK 경로와 함께 생깁니다.
4. 팀 드라이브의 `google-services.json`을 `android/app/`에 넣습니다.
5. `android/local.properties`에 아래 키를 추가합니다. 값은 팀 드라이브의 "local.properties 값.txt"에서 복사하세요.

   ```properties
   USE_EMULATOR=true
   KAKAO_NATIVE_APP_KEY=...
   CLOUDINARY_CLOUD_NAME=...
   CLOUDINARY_UPLOAD_PRESET=...
   ```

   | 키 | 하는 일 | 안 적으면 |
   | --- | --- | --- |
   | `USE_EMULATOR` | `true` = Firebase 에뮬레이터, `false` = 진짜 Firebase + Cloudinary | `false`로 처리되어 **진짜 Firebase에 붙습니다.** 평소엔 꼭 `true`로 적으세요. |
   | `KAKAO_NATIVE_APP_KEY` | 카카오맵 SDK 키 | 카카오맵 SDK를 초기화하지 않아 지도가 안 뜹니다. |
   | `CLOUDINARY_CLOUD_NAME` · `CLOUDINARY_UPLOAD_PRESET` | 진짜 Firebase 모드에서 사진을 올릴 곳 | Firebase 에뮬레이터 모드는 상관없고, `USE_EMULATOR=false`일 때 사진 업로드가 안 됩니다. |
   | `EMULATOR_HOST` (선택) | Firebase 에뮬레이터 주소. 기본 `127.0.0.1` | 적지 않는 것을 권장합니다. 앱은 `127.0.0.1` · `localhost`에만 http 통신을 허용해서, 다른 주소로 바꾸면 붙지 않습니다. |

   > `google-services.json`과 `local.properties`는 `.gitignore`에 들어 있어 커밋되지 않습니다. 키 값을 코드나 커밋, 채팅방에 붙여 넣지 마세요.

6. 앱을 돌릴 기기를 준비하고 ▶️ Run `app`을 한 번 실행합니다. 디버그 서명 키(`~/.android/debug.keystore`)가 이때 만들어집니다. 아직 Firebase 에뮬레이터를 켜지 않았어도 괜찮습니다.
   - **Android 에뮬레이터(가상 기기)**: Android Studio 상단 기기 목록이 비어 있으면 Device Manager에서 가상 기기를 하나 만듭니다.
   - **테스트 폰**: 폰의 개발자 옵션에서 USB 디버깅을 켜고 USB로 연결합니다. 개발자 옵션이 안 보이면 설정의 휴대전화 정보에서 빌드 번호를 여러 번 눌러 켭니다.
7. 카카오맵 키 해시와 Google 로그인 SHA-1을 뽑아 **안재인에게 보냅니다.** 두 값은 노트북(디버그 키)마다 달라서, 새 노트북이면 꼭 다시 등록해야 합니다.

   ```bash
   # 카카오맵 키 해시
   keytool -exportcert -alias androiddebugkey -keystore ~/.android/debug.keystore -storepass android | openssl sha1 -binary | openssl base64
   # Google 로그인 SHA-1
   keytool -list -v -alias androiddebugkey -keystore ~/.android/debug.keystore -storepass android | grep SHA1
   ```

   > **Windows**: `openssl` · `grep` · `~`가 필요해서 Git Bash에서 실행합니다. `~`는 `C:\Users\<사용자 이름>`입니다.

   `keytool`이 없다고 나오면 [자주 막히는 문제](#keytool을-찾을-수-없다고-나옴)를 보세요.

   받은 값은 안재인이 이렇게 등록합니다.
   - 키 해시: 카카오 개발자 콘솔 > 플랫폼 키 > Default Native AppKey의 Android 칸
   - SHA-1: Firebase 프로젝트 설정 > 내 앱 > "SHA 인증서 지문"의 "디지털 지문 추가". 추가한 뒤 `google-services.json`을 다시 내려받아 팀 드라이브 파일을 바꿉니다. **팀원도 팀 드라이브에서 새 파일을 받아 `android/app/`의 파일을 바꿔야 합니다.**

   등록을 기다리지 않고 2번으로 넘어가도 됩니다. 지도와 Google 로그인만 등록이 끝난 뒤에 동작합니다.

## 2. 개발할 때마다

터미널을 두 개 씁니다. 하나는 Firebase 에뮬레이터가 계속 차지하고, 다른 하나로 기기를 연결합니다.

1. **터미널 1**: 레포 루트에서 Firebase 에뮬레이터를 켭니다. 개발하는 동안 켜 둡니다. 켤 때 공용 가짜 데이터를 불러옵니다.

   ```bash
   cd firebase
   firebase emulators:start --config firebase.emulator.json --project walklog-e37fa --import ./seed
   ```

   - `firebase.emulator.json`은 포트와 개발용 규칙(전부 허용)을 씁니다. 그냥 `firebase.json`은 진짜 Firebase에 배포하는 설정이라 `--config`를 빼먹지 마세요.
   - 포트: Auth 9099 · Firestore 8080 · Storage 9199 · Emulator UI 4000
   - Firebase 에뮬레이터를 끄면 그 안의 데이터(가입한 계정, 로그, 사진)는 사라지고, 다시 켜면 가짜 데이터만 있는 처음 상태로 돌아갑니다.
   - `--export-on-exit`는 붙이지 마세요. 공용 가짜 데이터가 덮어써집니다. 가짜 데이터 내용 · 시험용 계정 · 다시 만드는 법은 [firebase/SEED.md](./firebase/SEED.md).

2. **터미널 2**: 새 터미널을 열고 레포 루트로 갑니다. 기기(Android 에뮬레이터 · 테스트 폰)를 켜거나 USB로 연결한 뒤 포트를 잇습니다. **기기를 켜거나 다시 연결할 때마다** 실행하세요.

   macOS:

   ```bash
   ./firebase/connect-device.sh
   ```

   **Windows**: 스크립트 대신 PowerShell에서 아래를 실행합니다(Android SDK를 기본 위치에 설치했다고 가정).

   ```powershell
   $adb = "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe"
   & $adb reverse tcp:9099 tcp:9099
   & $adb reverse tcp:8080 tcp:8080
   & $adb reverse tcp:9199 tcp:9199
   ```

   - 왜 이렇게 하나요? targetSdk 37부터 앱이 10.0.2.2 같은 로컬 네트워크 주소에 붙으려면 별도 권한이 필요합니다. 그래서 `adb reverse`로 노트북 포트를 기기에 이어 두고, 앱은 기기 자신(127.0.0.1)으로 접속합니다.
   - 스크립트는 PATH에 있는 `adb`를 쓰고, 없으면 `$ANDROID_HOME/platform-tools/adb`(설정이 없으면 macOS 기본 SDK 경로)를 씁니다.
   - 성공하면 스크립트 마지막에 이어진 포트 세 줄이 보입니다. Windows는 `& $adb reverse --list`로 확인하세요.

3. Android Studio에서 ▶️ Run `app`
4. 데이터 보기: http://127.0.0.1:4000 (Firebase Emulator UI). 화면이 Firebase에 붙고 나면 가입한 계정, Firestore 문서, 올린 사진이 여기에 보입니다.

> **Windows**: 터미널에서 Gradle을 돌릴 땐 `./gradlew` 대신 `gradlew.bat`을 씁니다(PowerShell · Android Studio 터미널).
>
> ```powershell
> cd android
> .\gradlew.bat assembleDebug
> ```

## 3. 진짜 Firebase로 맞춰 보기 · 시연

팀원끼리 같은 데이터로 맞춰 보거나 시연할 때만 진짜 Firebase(Spark 요금제, 프로젝트 `walklog-e37fa`)에 붙습니다.

1. `android/local.properties`에서 `USE_EMULATOR=false`로 바꿉니다.
2. ▶️ Run `app`으로 다시 빌드해 실행합니다. 이 모드에선 Firebase 에뮬레이터와 `connect-device.sh`가 필요 없습니다.
3. 끝나면 `USE_EMULATOR=true`로 되돌립니다.

| | Firebase 에뮬레이터 (`true`) | 진짜 Firebase (`false`) |
| --- | --- | --- |
| 로그인 · Firestore | 내 노트북의 Firebase 에뮬레이터 | `walklog-e37fa` (팀 전체가 같은 데이터를 씀) |
| 사진 | Firebase 에뮬레이터 Storage | Cloudinary (`CLOUDINARY_*` 키 필요) |
| 보안 규칙 | `firestore.dev.rules` · `storage.dev.rules` (전부 허용) | `firestore.rules` (Storage 안 씀) |

- 진짜 Firebase Storage는 카드 등록이 필요해서 쓰지 않습니다. 배포용 `storage.rules`도 전부 막아 두었습니다.
- 진짜 Firebase에선 팀원 모두가 같은 데이터를 봅니다. 테스트 데이터를 지울 땐 남의 것인지 확인하세요.

### 시연 전에 색인 올리기

Firebase 에뮬레이터는 색인 없이도 쿼리가 돌아서 놓치기 쉽습니다. 시연 전에 프로젝트 권한이 있는 사람이 `firebase/`에서 Firestore 색인을 올려야 합니다.

```bash
cd firebase
firebase login
firebase deploy --only firestore:indexes --project walklog-e37fa
```

색인은 `firebase/firestore.indexes.json`에 있습니다. 쿼리를 새로 만들면서 색인을 추가했다면 다시 올려야 합니다.

### 보안 규칙 기한

진짜 Firebase의 Firestore 규칙은 **2026-12-14까지** 누구나 읽고 쓸 수 있게 열려 있고, 그 뒤로는 로그인한 사람만 읽고 쓸 수 있습니다. 제대로 된 보안 규칙은 아직 만들지 않았습니다.

## 4. 자주 막히는 문제

### 지금 생길 수 있는 문제

#### `keytool`을 찾을 수 없다고 나옴

- **원인**: Java가 PATH에 없습니다.
- **해결**: 1-7번의 두 명령에서 맨 앞 `keytool`을 Android Studio에 들어 있는 `keytool` 경로로 바꿔 실행합니다. 경로에 공백이 있어 따옴표로 감쌉니다.

  ```bash
  # macOS
  "/Applications/Android Studio.app/Contents/jbr/Contents/Home/bin/keytool"
  # Windows (Git Bash)
  "/c/Program Files/Android/Android Studio/jbr/bin/keytool"
  ```

#### `debug.keystore` 파일이 없다고 나옴

- **원인**: 이 노트북에서 앱을 아직 한 번도 Run하지 않았습니다. 디버그 서명 키는 첫 빌드 때 만들어집니다.
- **해결**: [1-6번](#1-처음-한-번)대로 ▶️ Run `app`을 한 번 한 뒤 다시 뽑습니다. Windows는 `C:\Users\<사용자 이름>\.android\debug.keystore`에 생겼는지 확인하세요.

#### PowerShell에서 `npm` · `firebase`가 스크립트 실행 오류로 안 됨

- **증상** (Windows): "이 시스템에서 스크립트를 실행할 수 없으므로…(running scripts is disabled)" 오류
- **원인**: PowerShell 실행 정책이 `.ps1` 스크립트를 막고 있습니다.
- **해결**: `npm.cmd` · `firebase.cmd`로 부르거나, Git Bash · 명령 프롬프트(cmd)에서 실행합니다. 실행 정책을 바꾸는 방법(`Set-ExecutionPolicy -Scope CurrentUser RemoteSigned`)도 있지만, 보안 설정이라 본인이 판단해서 하세요.

#### Windows Git Bash에서 gradlew나 connect-device 스크립트가 실패함

- **증상** (Windows): `./gradlew`나 `./firebase/connect-device.sh` 파일은 있는데 실행이 안 되고, 줄 끝 문자(`\r`)와 관련된 오류가 납니다.
- **원인**: Windows용 Git이 clone할 때 줄바꿈을 CRLF로 바꿔서, 셸 스크립트가 깨집니다.
- **해결**:
  - Gradle: Android Studio 버튼을 쓰거나, PowerShell · Android Studio 터미널에서 `cd android` 후 `.\gradlew.bat assembleDebug`처럼 실행합니다.
  - 기기 연결: [2-2번](#2-개발할-때마다)의 PowerShell `adb reverse` 세 줄을 씁니다.

#### 포트 연결(`adb reverse`)이 실패함

- 기기가 없다는 오류: Android 에뮬레이터(가상 기기)를 먼저 켜거나 테스트 폰을 연결하고, Android Studio 기기 목록에 보이는지 확인합니다. 테스트 폰은 USB 디버깅이 켜져 있어야 합니다.
- 기기가 여러 대라서 하나를 고르라는 오류: 하나만 연결하거나, `adb devices`로 기기 ID를 확인해 `ANDROID_SERIAL=<기기 ID> ./firebase/connect-device.sh`처럼 지정합니다. Windows는 세 줄을 `& $adb -s <기기 ID> reverse …`로 바꿔 실행합니다.

#### Firebase 에뮬레이터가 Java 때문에 안 켜짐

- **원인**: 터미널에서 쓰는 Java 버전이 21보다 낮습니다. Android Studio에 들어 있는 JDK와는 별개입니다.
- **해결**: [0. 준비물](#0-준비물)대로 Java 21 이상을 설치하고, 새 터미널에서 `java -version`이 21 이상인지 확인합니다.

#### develop을 받은 뒤 Kotlin 플러그인을 업데이트하라는 안내가 뜸

- **원인**: 프로젝트가 쓰는 Kotlin 버전(`android/gradle/libs.versions.toml`의 `kotlin`)이 올라갔는데, Android Studio의 Kotlin 플러그인이 그보다 낮습니다.
- **해결**: 안내대로 업데이트하고 Android Studio를 다시 시작한 뒤 Gradle Sync를 합니다.

### 화면이 붙은 뒤 생길 수 있는 문제

지금은 화면이 자리표시자라 아래 문제는 아직 드러나지 않습니다. 화면이 Firebase · 지도 · 로그인에 붙고 나면 확인하세요.

#### 앱이 Firebase 에뮬레이터에 못 붙음 (로그인 · 저장이 한참 걸리다 실패)

- **원인과 해결**: 아래를 차례로 확인합니다.
  1. 터미널 1의 Firebase 에뮬레이터가 켜져 있나요? 꺼졌으면 [2-1번](#2-개발할-때마다)대로 다시 켭니다. http://127.0.0.1:4000 이 열리면 켜진 것입니다.
  2. 기기를 다시 켜거나 USB를 다시 꽂았나요? 그때마다 포트 연결이 풀립니다. `connect-device.sh`(Windows는 `adb reverse` 세 줄)를 다시 실행합니다.
  3. `local.properties`에 `EMULATOR_HOST`를 다른 값으로 적었나요? 지우고 다시 Run합니다.

#### Firebase Emulator UI엔 아무것도 안 생기고, 데이터가 진짜 Firebase에 쌓임

- **원인**: `local.properties`에 `USE_EMULATOR=true`가 없거나 오타가 났습니다. 이 값이 없으면 `false`로 처리되어 진짜 Firebase에 붙습니다.
- **해결**: `USE_EMULATOR=true`로 적고 다시 Run합니다. 진짜 Firebase에 잘못 쌓인 테스트 데이터는 안재인에게 알려 주세요.

#### 카카오맵이 빈 화면이거나 인증 오류가 남

- **원인**: 이 노트북의 키 해시가 카카오 개발자 콘솔에 등록되지 않았습니다. 노트북을 바꾸거나 `debug.keystore`를 새로 만들었으면 키 해시도 바뀝니다. `KAKAO_NATIVE_APP_KEY`가 비어 있어도 지도가 안 뜹니다.
- **해결**: `local.properties`에 키가 있는지 확인하고, [1-7번](#1-처음-한-번)대로 키 해시를 다시 뽑아 안재인에게 등록을 부탁합니다.

#### Google 로그인이 실패함

- **원인**: 이 노트북의 SHA-1이 Firebase에 등록되지 않았거나, 등록한 뒤 `google-services.json`을 새로 받지 않았습니다.
- **해결**: [1-7번](#1-처음-한-번)대로 SHA-1을 보내 등록을 부탁하고, 등록이 끝나면 팀 드라이브의 새 `google-services.json`으로 `android/app/`의 파일을 바꾼 뒤 다시 Run합니다.

#### 진짜 Firebase에서만 목록이 안 뜨고 색인이 필요하다는 오류가 남

- **원인**: Firestore 복합 색인이 진짜 Firebase에 올라가지 않았습니다. Firebase 에뮬레이터는 색인 없이도 돌아서 평소엔 드러나지 않습니다.
- **해결**: 권한 있는 사람이 [시연 전에 색인 올리기](#시연-전에-색인-올리기)를 실행합니다.

#### 진짜 Firebase에서 권한 거부 오류가 남

- **원인**: 2026-12-14부터는 로그인한 사람만 Firestore를 읽고 쓸 수 있습니다.
- **해결**: 로그인한 상태에서 다시 해 봅니다. 규칙은 `firebase/firestore.rules`에 있습니다.
