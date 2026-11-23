# 나나컴퍼니 안드로이드 앱 (WebView)

<https://steadystroke.github.io/stock/> 을 전체화면으로 보여 주는 안드로이드 앱입니다.
안드로이드 스튜디오 없이 **GitHub Actions** 가 APK 를 빌드합니다.

## APK 만드는 법

1. GitHub 에 새 저장소를 만듭니다. (이름은 영문으로, 예: `nana-stock-android`)
2. 이 폴더에서 아래 명령을 실행합니다. `<아이디>` 와 `<저장소>` 는 본인 것으로 바꿉니다.

   ```bash
   git init -b main
   git add .
   git commit -m "나나컴퍼니 웹뷰 앱"
   git remote add origin https://github.com/<아이디>/<저장소>.git
   git push -u origin main
   ```

3. 저장소의 **Actions** 탭에서 "Android APK 빌드" 가 끝나기를 기다립니다. (3~5분)
4. 저장소의 **Releases** 에 올라온 `nana-stock.apk` 를 폰에서 내려받아 설치합니다.
   - 고정 주소: `https://github.com/<아이디>/<저장소>/releases/latest/download/nana-stock.apk`
   - 처음 설치할 때는 폰에서 "출처를 알 수 없는 앱 설치" 를 허용해야 합니다.

이후에는 파일을 고쳐 `git push` 할 때마다 새 APK 가 자동으로 만들어지고,
폰에 깔린 앱 위에 그대로 덮어 설치할 수 있습니다.

## 자주 바꾸는 곳

| 바꾸고 싶은 것 | 파일 |
| --- | --- |
| 보여 줄 사이트 주소 | `app/src/main/java/io/github/steadystroke/stock/MainActivity.java` 의 `START_URL`, `APP_HOST` |
| 앱 이름 | `app/src/main/res/values/strings.xml` 의 `app_name` |
| 앱 아이콘 | `app/src/main/res/drawable/ic_launcher_*.xml` |
| 패키지 이름 · 최소 안드로이드 버전 | `app/build.gradle` |

## 알아 둘 점

- 안드로이드 8.0 이상에서 설치됩니다.
- 상태 표시줄과 내비게이션 바를 숨기는 전체화면입니다. 화면 가장자리를 쓸면 잠깐 나타납니다.
- **Google 로그인은 앱 안에서 되지 않습니다.** Google 이 웹뷰에서의 로그인을 정책으로 막고 있습니다.
  로그인 버튼을 누르면 안내와 함께 브라우저에서 여는 버튼이 나옵니다.
- `keystore/app-signing.p12` 는 앱 서명 키입니다. 매번 같은 키로 서명해야 덮어 설치가 되므로 저장소에 함께 넣어 두었습니다.
  비밀번호가 `app/build.gradle` 에 적혀 있으니, 저장소를 공개로 두면 누구나 이 키로 서명할 수 있습니다.
  개인용 앱이면 문제없지만, 스토어에 올리거나 널리 배포할 앱이라면 비공개 저장소를 쓰거나 키를 GitHub Secrets 로 옮기세요.
  이 파일을 지우거나 바꾸면 기존 앱을 삭제한 뒤 다시 설치해야 합니다.
