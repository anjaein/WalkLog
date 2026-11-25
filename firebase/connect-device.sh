#!/bin/sh
# 안드로이드 기기(에뮬레이터 · USB로 연결한 폰)에서 127.0.0.1로 노트북의 Firebase 에뮬레이터에 닿도록 포트를 이어 준다.
# 기기를 새로 켜거나 다시 연결할 때마다 한 번 실행한다. 포트는 firebase.emulator.json과 같아야 한다.
ADB="${ANDROID_HOME:-$HOME/Library/Android/sdk}/platform-tools/adb"
command -v adb >/dev/null 2>&1 && ADB=adb
for port in 9099 8080 9199; do
  "$ADB" reverse "tcp:$port" "tcp:$port" || exit 1
done
"$ADB" reverse --list
