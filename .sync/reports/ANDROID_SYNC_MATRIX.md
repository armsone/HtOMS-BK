# Android Sync Matrix — HtOMS Brief

- 캐노니컬 계약: **Apple 저장소** `HtOMSBrief/.sync/product-contract.yaml` (이 저장소는 계약을 복제하지 않는다)
- 작성일: 2026-08-25
- 상태 정의: `implemented_source_only` = Android 소스 작성과 빌드·단위 테스트·린트 검증 완료, 런타임 시각 증거 대기.
  이 문서의 모든 능력은 **런타임/시각 검증 pending** 상태다.

| Capability | Apple 소스 근거 | Android 소스 근거 | Android 테스트 근거 | 상태 |
| --- | --- | --- | --- | --- |
| CAP-AUTH-01 스플릿 플랩 로그인 | `HtOMSBrief/LoginView.swift` | `app/src/main/java/com/htoms/brief/ui/LoginScreen.kt` | `androidTest .../LoginScreenCatalogTest.kt` | implemented_source_only |
| CAP-AUTH-02 인증 API·세션 핸드셰이크 | `HtOMSBrief/OMSAPI.swift`, `AuthSession.swift` | `api/OMSAPIClient.kt` (signIn, 401/403→Unauthorized) | `test .../OMSAPIClientTest.kt` | implemented_source_only |
| CAP-AUTH-03 하드웨어 보안 저장·세션 지속 | `HtOMSBrief/SecureStore.swift`, `AuthSession.swift` | `security/KeystoreSecureStore.kt`, `auth/SessionController.kt` | `test .../SessionControllerTest.kt` | implemented_source_only |
| CAP-AUTH-04 로그아웃 정리 | `HtOMSBriefApp.swift`, `Shared/WidgetSnapshotStore.swift` | `MainActivity.kt`(logout→store 삭제→위젯 무효화) | `SessionControllerTest`, `WidgetSnapshotStoreTest` | implemented_source_only |
| CAP-CATALOG-01 결정적 UI 카탈로그 | `HtOMSBriefApp.swift`, `BriefProviding.swift` | `MainActivity.kt`(--ez ui-catalog-*), `provider/SampleBriefProvider.kt` | `test .../SampleBriefProviderTest.kt` | implemented_source_only |
| CAP-CORE-01 단일 세로 스크롤 브리프 | `BriefRootView.swift`, `Components.swift` | `ui/BriefRootScreen.kt`, `ui/Components.kt` (max 1000dp, 36dp 간격, 헤더 바) | (계측 캡처 pending) | implemented_source_only |
| CAP-SYNC-01 10분 자동 갱신·카운트다운 | `BriefRootView.swift`, `OverviewPage.swift` | `ui/BriefViewModel.kt` (600s, 1s tick, MM:SS) | `test .../BriefViewModelTest.kt` | implemented_source_only |
| CAP-SYNC-02 병렬 로드·우아한 저하 | `OMSAPI.swift`, `BriefRootView.swift` | `api/RemoteBriefProvider.kt` (async 병렬, 보조 실패→빈 값, Unauthorized 전파), `BriefRootScreen.kt` (배너/전면 오류) | `test .../RemoteBriefProviderResilienceTest.kt` | implemented_source_only |
| CAP-API-01 읽기 전용 OMS REST | `OMSAPI.swift` | `api/OMSAPIClient.kt` ReadEndpoint 화이트리스트(GET 전용) | `OMSAPIClientTest` (mutation route 부재 포함) | implemented_source_only |
| CAP-API-02 Firestore 배송 집계 | `OMSAPI.swift` DeliveryAggregateClient | `api/DeliveryAggregateClient.kt` (상태 분류·날짜 범위·PII 미저장) | `test .../DeliveryAggregateClientTest.kt` | implemented_source_only |
| CAP-DATA-01 매출 계산·등급 | `OMSAPI.swift`, `Theme.swift` | `api/RemoteBriefProvider.kt` (만원 반올림, 채널 비중, 월 진행률, 등급 임계값, 상위 5+기타), `theme/BriefFormat.kt` | `RemoteBriefProviderMapperTest`, `BriefFormatTest` | implemented_source_only |
| CAP-DATA-02 서버 하트비트 10분 임계 | `OMSAPI.swift` serverStatuses | `api/RemoteBriefProvider.kt` (4개 봇, ≤600s, 다중 날짜 포맷, Asia/Seoul) | `RemoteBriefProviderMapperTest` (경계 600s/601s) | implemented_source_only |
| CAP-PAGE-01 매출 요약 섹션 | `OverviewPage.swift` | `ui/OverviewPage.kt` (KPI·REFRESH·SERVER·채널 바·적응형 2열 그리드·도넛) | (시각 pending) | implemented_source_only |
| CAP-PAGE-02 시간대별 비교 차트 | `DayTrendPage.swift` | `ui/DayTrendPage.kt` (±1 도메인, 2시간 축, 점선 1.15dp[4,4]/실선 1.6dp, 누락값 선 끊김) | (시각 pending; 데이터는 매퍼 테스트) | implemented_source_only |
| CAP-PAGE-03 월간 일별 차트 | `MonthTrendPage.swift` | `ui/MonthTrendPage.kt` (평균 점선[5,4], 0매출 적색 점, 6일 축) | (시각 pending) | implemented_source_only |
| CAP-PAGE-04 택배 현황 | `DeliveryPage.swift`, `Theme.swift` | `ui/DeliveryPage.kt` (총 건수 44sp, 8단계 상태색, 145dp 적응 그리드) | (시각 pending; 집계는 단위 테스트) | implemented_source_only |
| CAP-WIDGET-01 위젯 스냅샷 브리지 | `Shared/DashboardWidgetSnapshot.swift`, `WidgetSnapshotStore.swift` | `model/DashboardWidgetSnapshot.kt`, `widget/WidgetSnapshotStore.kt` (schemaVersion 1, 앱 전용 파일, 로그아웃 시 삭제) | `WidgetSnapshotStoreTest` (손상·버전·자격증명 부재 포함) | implemented_source_only |
| CAP-WIDGET-02 반응형 홈 위젯 | `HtOMSWidget/SalesBoardWidget.swift` | `widget/SalesBoardWidget.kt` (Small 8행/Medium 2열/Large 6행+타임스탬프, SizeMode.Responsive), `widget/WidgetFormat.kt` | `WidgetFormatTest` | implemented_source_only |

## 명시적 미검증 항목
- 전 능력의 **런타임 실행·시각 비교**: Android 캡처가 없어 pending. 어떤 항목도 matched 아님.
- JVM 단위 테스트·디버그 빌드·린트는 통과. Compose 계측 테스트와 실제 화면 캡처는 기기 실행 전이라 미검증.

## 의도적 차이 요약
계약 `os_limitations_and_intentional_differences`(DIFF-SEC-01, DIFF-WIDGET-01, DIFF-TV-01)를 따름.
추가로 Android 위젯 자동 갱신 주기는 플랫폼 최소값(30분)을 사용하고 앱 갱신 성공 시 즉시 갱신으로 보완.
