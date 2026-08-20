# HtOMS iOS 매치업 검증 보고서

## 결론

- iOS 앱 화면 5종을 iPhone 17 Pro와 iPad Pro 11-inch (M5)에서 각각 캡처했다.
- 실제 WidgetKit 갤러리에서 소형 위젯을 캡처했다. 소형 한 장에 금일(날짜)·전일·당월(월)·전월·평균·목표(달성률)·시간·서버가 모두 들어가며 잘림이 없다.
- 시간 그래프의 좌표 이상은 두 LineMark가 동일 계열로 연결된 것이 주원인이었다. 시간축을 숫자형으로 바꾸고 두 series를 분리했으며 선 굵기를 회색 1.15pt·오렌지 1.6pt로 낮췄다.
- 기존 OMS 웹 소스의 비교 계약은 `30일 합계` 대 `오늘 ×10`이며 앱에서 이 의미와 `만원 비교값`을 숨기지 않고 표시한다.
- 이 작업공간에는 Android 구현이 없으므로 교차 플랫폼 일치 여부는 검증하지 않았다. 현재 결과는 iOS 기준 카탈로그와 향후 Android 구현용 상태 지도다.

## 증거 품질

- 원본 첨부 이미지는 원본 경로·픽셀 크기·SHA-256을 `reference-manifest.csv`에 기록했다.
- 앱 카탈로그는 고정 `SampleBriefProvider`, 한국어, 다크 모드, 세로 방향, iOS 26.5에서 캡처했다.
- iPhone 캡처는 1206×2622, iPad 캡처는 1668×2420이다.
- 정확한 선 굵기·색·값 변환은 소스에서 확인했고 배치·잘림·계층은 런타임 PNG에서 확인했다.
- 상태바와 홈 인디케이터는 iOS 소유 영역이다. 앱 콘텐츠는 별도 임의 확대·비율 변형 없이 판정했다.

## 카탈로그

- `screenshots/iphone_17_pro/`: 로그인, 요약, 시간, 월간, 택배, 소형 위젯
- `screenshots/ipad_pro_11-inch_(m5)/`: 로그인, 요약, 시간, 월간, 택배
- `manifest-iphone_17_pro.csv`, `manifest-ipad_pro_11-inch_(m5).csv`: 캡처 크기와 SHA-256
- `reference-manifest.csv`: 사용자 원본 자료 크기와 SHA-256
- `matrix.csv`: 화면·요소별 분류와 검증 상태

## 재생성

```sh
scripts/capture_ui_catalog.sh
CATALOG_DEVICE='iPad Pro 11-inch (M5)' scripts/capture_ui_catalog.sh
```

WidgetKit 소형 캡처는 최신 빌드를 시뮬레이터에 설치한 뒤 `홈 화면 편집 → 위젯 추가 → HtOMSBrief → 작게`에서 확인한다.

## 검증

- `xcodebuild ... test`: XCTest 16개, Swift Testing 2개, 합계 18개 통과.
- 업무 데이터 API 안전성 테스트: 인증을 제외한 업무 endpoint는 GET 전용이며 mutation route가 whitelist에 없음을 확인했다.
- 소스 수정과 로컬 시뮬레이터 검증만 수행했다. 커밋·푸시·실기기 설치·배포는 수행하지 않았다.
