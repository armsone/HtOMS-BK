# HtOMS Brief — Android Matchup Report (초안)

작성일: 2026-08-25 · 상태: **implemented_source_only** (빌드·단위 테스트·린트 통과, 런타임 시각 검증 전)

## 요약
- Apple 캐노니컬 계약(`HtOMSBrief/.sync/product-contract.yaml`)의 18개 능력을 Android
  Jetpack Compose 단일 앱(폰·태블릿·리사이즈 창·Google TV)과 Glance 위젯으로 구현했다.
- 본 리포트의 어떤 항목도 **matched / visual parity를 주장하지 않는다.** Android 런타임
  캡처가 없으므로 Apple `artifacts/matchup` 캡처와 쌍을 이룬 비교 증거가 존재하지 않는다.
- 알고리즘·매핑·상태 전이는 Apple 테스트 fixture를 이식한 JVM 단위 테스트로 검증했고,
  디버그 APK 빌드와 Android Lint도 통과했다.

## 증거 상태
| 구분 | 상태 |
| --- | --- |
| Android 소스 구현 | 완료 (capability별 매핑은 `.parity/ledger.json`) |
| JVM 단위 테스트 | 통과 |
| Compose 계측 테스트 | 작성 완료(로그인 카탈로그), 실행 전 |
| 런타임 스크린샷 | 없음 (`manifest.csv` 전 행 pending) |
| 시각 픽셀 비교 | 없음 |

## 의도적 플랫폼 차이 (계약 명시)
- DIFF-SEC-01: Keychain → Android Keystore(AES/GCM) 암호화 저장. 로그인 문구도
  계약에 따라 "Keystore 보관"으로 표기.
- DIFF-WIDGET-01: WidgetKit 15분 타임라인 → Glance + `updatePeriodMillis=1800000`
  (Android 최소 30분) + 앱 갱신 성공 시 즉시 `updateAll`. 15분 주기는 Android 정책상 불가.
- DIFF-TV-01: Leanback 런처·D-pad 포커스 테두리(2dp 오렌지)·1.25x 타이포 스케일.
- 차트: SwiftUI Charts 대신 Canvas 직접 렌더링. 선 두께·대시·점 크기·색·축 규칙은
  소스 수치를 이식했으나 픽셀 검증 전.

## 다음 단계 (Codex)
1. 대표 기기 또는 명시적으로 시작한 에뮬레이터(폰·태블릿·TV)에서 `manifest.csv`의 6개 상태 캡처.
2. Apple 캡처와 쌍 비교 후 `matrix.csv`의 Android observed/Difference 갱신.
