# 컨벤션

> 이 문서는 팀의 협업 규칙(커밋·브랜치·네이밍·PR)을 한곳에 모은 가이드입니다.
> 스택: **Kotlin + Jetpack Compose (Android) · Firebase**
>
> 자동으로 강제되는 규칙은 없습니다. 다만 **⚠️ 라벨링 필수 규칙**은 지키지 않으면 라벨이 안 붙으니 꼭 지켜주세요.

---

## ⚠️ 라벨링 필수 규칙

### 1. 코드는 정해진 폴더에 넣기

PR 라벨러는 **파일 경로**를 보고 영역 라벨을 붙입니다. 폴더 이름도 아래와 정확히 같아야 합니다.

| 폴더 (`android/app/src/main/java/.../walklog/`) | 라벨       |
| :---------------------------------------------- | :--------- |
| `feature/auth/`                                 | `auth`     |
| `feature/record/`                               | `record`   |
| `feature/photomap/`                             | `photomap` |
| `feature/discover/`                             | `discover` |
| `feature/mypage/`                               | `mypage`   |
| `ui/`, `res/`                                   | `ui`       |
| `data/`                                         | `data`     |
| `domain/`                                       | `domain`   |

그 외 `firebase`, `config`, `dependencies`, `documentation` 라벨은 파일 종류에 따라 자동으로 붙습니다.

### 2. PR 템플릿의 변경 유형은 체크만 하기

`type:` 라벨은 PR 본문의 **변경 유형** 체크박스를 읽어서 붙습니다.

```md
- [x] ✨ `feat` - 새로운 기능 추가
```

- `[ ]` → `[x]` 로 바꾸는 것만 해주세요.
- 줄을 지우거나, 백틱(`` `feat` ``)을 없애거나, 문구를 바꾸면 인식되지 않습니다.
- 체크를 풀면 라벨도 자동으로 떨어집니다.

---

## 0. 워크플로우

```
이슈 생성 → 브랜치 생성 → 작업 & 커밋 → PR 생성 → 리뷰 → develop 머지
```

- 이슈 템플릿(feat · fix · refactor · design)을 고르면 `type:` 라벨이 자동으로 붙습니다.
- 그 외 유형(docs · perf · test · chore)은 Blank issue로 만들고 `type:` 라벨을 직접 달아주세요.
- 이슈 제목에는 `feat:` 같은 접두어를 붙이지 않습니다. (라벨로 구분)
- PR 본문에 `Closes #이슈번호` 를 적으면 develop 머지 시 이슈가 자동으로 닫힙니다.

---

## 1. 커밋 컨벤션 (권장)

```
type: 메시지
```

예시: `feat: 로그인 화면 구현`

- 타입은 **소문자만** 사용합니다.
- 한 커밋은 한 가지 의미 단위로 명령형/완료형으로 작성합니다.

### 타입 (8종)

PR 템플릿의 변경 유형, `type:` 라벨과 동일합니다.

| 타입       | 의미                   | 예시                        |
| :--------- | :--------------------- | :-------------------------- |
| `feat`     | 새로운 기능 추가       | 화면 개발, Firebase 연동    |
| `fix`      | 버그 수정              | 로직 오류, 크래시 수정      |
| `design`   | UI 디자인 변경         | 레이아웃, 색상, 테마        |
| `docs`     | 문서 수정              | README, 컨벤션              |
| `refactor` | 리팩토링 (기능 변화 X) | 구조 개선, 이름 변경        |
| `perf`     | 성능 개선              | 지도 로딩, 리컴포지션 최적화 |
| `test`     | 테스트 코드            | 테스트 추가/수정            |
| `chore`    | 자잘한 수정/설정       | 의존성, Gradle, GitHub 설정 |

---

## 2. 브랜치 컨벤션 (권장)

```
type/설명-이슈번호
```

예시: `feat/login-12`, `fix/gps-jump-34`

| 브랜치      | 역할                                                        |
| :---------- | :---------------------------------------------------------- |
| `main`      | 데모 · 제출용 안정 버전. `develop`에서만 머지               |
| `develop`   | 기본 브랜치. 기능이 모이는 통합 브랜치이자 평소 작업의 기준점 |
| 작업 브랜치 | 기능/수정 단위로 `develop`에서 분기                          |

타입은 커밋과 동일한 8종을 사용합니다.

---

## 3. 네이밍 컨벤션 (권장)

[Kotlin 공식 코딩 컨벤션](https://kotlinlang.org/docs/coding-conventions.html)을 따릅니다.

### 파일 / 패키지

| 대상          |     규칙     | 예시                    |
| :------------ | :----------: | :---------------------- |
| 패키지 (폴더) |   `소문자`   | `feature/photomap`      |
| Kotlin 파일   | `PascalCase` | `LoginScreen.kt`        |
| 리소스 파일   | `snake_case` | `ic_pin.xml`, `bg_card` |

### 코드 식별자

| 대상                 |     규칙      | 예시                         |
| :------------------- | :-----------: | :--------------------------- |
| 클래스 / 인터페이스  | `PascalCase`  | `WalkRepository`             |
| Composable 함수      | `PascalCase`  | `PhotoMapScreen()`           |
| 일반 함수            |  `camelCase`  | `calculateDistance()`        |
| 변수 / 프로퍼티      |  `camelCase`  | `userName`                   |
| 상수                 | `UPPER_CASE`  | `MAX_SPEED`                  |
| enum 멤버            | `UPPER_CASE`  | `Theme.NIGHT_VIEW`           |
| private backing 필드 | `_` + `camelCase` | `_uiState`               |

### 화면 단위 파일 이름

| 역할      | 접미사      | 예시                 |
| :-------- | :---------- | :------------------- |
| 화면      | `Screen`    | `LoginScreen`        |
| ViewModel | `ViewModel` | `LoginViewModel`     |
| UI 상태   | `UiState`   | `LoginUiState`       |
| 데이터    | `Repository`| `WalkLogRepository`  |
