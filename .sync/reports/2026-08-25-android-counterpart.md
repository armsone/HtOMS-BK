# HtOMS Brief Android counterpart report

- 작업 시간: 2026-08-25 10:03:28–11:25 KST
- Apple 기준 저장소: `/Users/armsone/git/HtOMSBrief` (`a6bcd8d`)
- Android 저장소: `/Users/armsone/git/HtOMSBrief-Android` (독립 Git, GitHub `HtOMS-BK/android`)
- 공개 제품 버전: `2.0.0`
- Android 표시 빌드: `202608251115` (versionCode `340515`)

## 완료

- 최종 폴더명과 Codex 프로젝트 연결을 `HtOMSBrief` / `HtOMSBrief-Android`로 정리했다.
- Android 휴대전화, 태블릿, Google TV를 지원하는 Compose 앱을 구현했다.
- 로그인, 세션 보안 저장, OMS 읽기 전용 조회, 배송 집계, 개요/일/월 차트, 10분 새로고침, Glance 위젯을 구현했다.
- `:app:testDebugUnitTest`, `:app:assembleReleaseQa`, `:app:lintDebug`가 통과했다.
- APK 메타데이터, Leanback 런처/배너, 선택 하드웨어 선언과 APK v2 서명을 확인했다.
- SM-F968N에 공개 후보를 데이터 유지 설치하고 MainActivity 실행과 versionCode 340515를 확인했다.
- GitHub Release `android-v340515`에 APK를 공개하고 실제 다운로드와 SHA-256을 확인했다.
- NasFinder.com에 Android · Google TV 공개 상태와 다운로드 버튼을 반영하고 Sites 버전 167을 운영 배포했다.
- 재사용 스킬 `android-counterpart`를 만들고 검증했다.

## 동기화 판정

- 제품 계약의 18개 기능은 Android 소스와 테스트 기준으로 구현됐다.
- 휴대전화 설치·실행은 확인했지만 결정적 화면 캡처 쌍이 없어 Matchup 18개 행은 모두 `implemented_source_only` 상태다.
- 따라서 기능 소스 구현은 완료했지만 렌더링 동일성과 전체 커플 동기화 게이트는 아직 완료로 판정하지 않는다.

## 검증 결과

- Android 단위 테스트: 통과
- Android 공개용 QA APK 조립: 통과
- Android lint: 오류 없이 통과(경고는 존재)
- Matchup 구조 검증: 통과, 18개 행 열림
- NasFinder.com 빌드 및 HtOMS 렌더링 대상 테스트: 통과
- NasFinder.com 전체 테스트: 기존 범위의 무관한 3개 실패가 남아 있음
- GitHub APK 직접 다운로드: 통과
- 홈페이지 다운로드 경로: 302로 검증된 GitHub APK 자산에 연결
- 운영 페이지 확인: `Android · Google TV`, `Android용 APK 다운로드`, code 340515와 SHA-256 표시

## 작업 중 오류와 처리

- 초기 임시 폴더명이 최종 이름과 달랐음: 최종 폴더로 변경하고 프로젝트 연결을 다시 확인했다.
- Gemini 작업 경로 실패 및 Claude 사용량 제한: 남은 구현을 로컬에서 검증하고 수정했다.
- AGP 9.3의 오래된 Kotlin 플러그인 충돌: 내장 Kotlin 구성으로 전환했다.
- Android SDK 경로 누락: Git 제외 `local.properties`에 로컬 SDK 경로를 연결했다.
- Glance 1.1.1 미지원 `defaultWeight`: 지원되는 고정 폭/간격 구성으로 수정했다.

## 남은 검증

- 태블릿·Google TV 실기기 설치·D-pad 실행
- iPhone·iPad와 Android의 결정적 화면 캡처 쌍을 이용한 18개 Matchup 판정

휴대전화 공개 릴리스와 홈페이지 다운로드 제공은 완료했지만 위 항목 전에는 전체 렌더링 매치업 완료로 판정하지 않는다.
