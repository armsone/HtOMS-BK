# HtOMS Brief — Android 구현 상태

작성일: 2026-08-25 · 전체 상태: **소스·단위 테스트·공개용 QA 빌드·린트 검증 완료**. 런타임 화면 매치업은 아직 열려 있다.

## 구성 기준선
| 항목 | 값 |
| --- | --- |
| 표시 이름 | HtOMS Brief |
| rootProject.name | HtOMSBriefAndroid |
| namespace / applicationId | com.htoms.brief |
| versionName | 2.0.0 (소스 표시 빌드 202608251115, versionCode 340515) |
| minSdk / compile·targetSdk | 26 / 37 |
| Java / AGP / Kotlin Compose plugin | 17 / 9.3.0 / 2.3.21 |
| UI | Jetpack Compose 단일 앱 (폰·태블릿·리사이즈 창·Google TV), Material3 다크 전용 |
| 위젯 | Jetpack Glance 1.1.1 (`SalesBoardGlanceWidgetReceiver`) |
| 네트워크 | 플랫폼 `HttpURLConnection` + coroutines (`HttpExecutor` 추상화, 추가 의존성 없음) |
| 보안 저장소 | Android Keystore AES/GCM 직접 구현 (`KeystoreSecureStore`) — deprecated androidx.security 미사용 |

## 경계·보안 결정
- 업무 엔드포인트는 `OMSAPIClient.ReadEndpoint` 화이트리스트로 GET만 표현 가능. 유일한 POST는 `/api/auth/signin`.
- 토큰은 Keystore 암호화 저장소(`auth-session` 키)에만 기록. 로그 출력 없음. 로그아웃 시 저장소·위젯 스냅샷 삭제 후 위젯 무효화.
- 위젯 스냅샷은 표시 전용 수치만 포함(자격 증명·PII 없음, `schemaVersion=1`, 앱 전용 filesDir).
- Firestore 배송 집계는 상태 합계만 파싱하며 주문·송장 식별자를 모델에 담지 않는다.
- 하드코딩 엔드포인트는 Apple 소스에 존재하는 제품 상수 2개뿐: `htoms.cafe24.com`, Firestore `ViewData` URL.

## 결정적 카탈로그 모드 (디버그 전용)
```
adb shell am start -n com.htoms.brief/.MainActivity --ez ui-catalog-login true
adb shell am start -n com.htoms.brief/.MainActivity --ez ui-catalog-brief true   # (ui-catalog-page 도 동일 동작)
```
`UnconnectedAuthService` / `SampleBriefProvider`(2026-08-20 Asia/Seoul fixture)로 네트워크·실데이터 접근 없이 렌더링.

## 통과한 검증 명령
```
cd /Users/armsone/git/HtOMSBrief-Android
./gradlew :app:testDebugUnitTest :app:assembleDebug
./gradlew :app:lintDebug
./gradlew :app:assembleReleaseQa
```
APK의 서명을 검증했고 `aapt2`로 Leanback 런처 노출, `tv_banner`, 터치스크린 required=false, small~xlarge 화면 지원을 확인했다.

## 알려진 미확정 사항 (Codex 확인 필요)
1. 차트는 SwiftUI Charts 대응 Canvas 직접 구현 — 축 눈금 알고리즘(`ChartSupport.yTicks`)은 Apple의 자동 눈금과 수치가 다를 수 있음(시각 비교로 판정).
2. Glance `cornerRadius`는 API 31+에서만 적용(하위 버전은 사각 셀).
3. 위젯 자동 갱신은 Android 최소 30분(`updatePeriodMillis=1800000`) + 앱 갱신 성공 시 즉시 `updateAll` — iOS 15분 타임라인과 의도적 차이.
4. Compose `ViewThatFits` 대응은 너비 임계값(서버 상태 360dp, 채널 범례 480dp) 휴리스틱 — 시각 비교 후 조정 가능.

## 산출물 위치
- 능력별 매핑·상태: `.parity/ledger.json`, `.sync/reports/ANDROID_SYNC_MATRIX.md`
- 매치업 스캐폴드: `artifacts/matchup/{manifest.csv, matrix.csv, MATCHUP_REPORT.md}` (캡처 전 pending)
