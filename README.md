# AI Decision Archive

사용자의 중요한 결정을 기록하고, AI 분석과 통계 대시보드를 통해 의사결정 패턴을 확인할 수 있는 Android 애플리케이션입니다.

## 프로젝트 개요

AI Decision Archive는 사용자가 내린 결정을 저장하고, 이후 회고하거나 분석할 수 있도록 돕는 앱입니다.
결정 내용은 로컬 DB에 저장되며, ChatGPT API와 날씨 API를 활용하여 결정 상황에 대한 AI 분석 결과를 제공합니다.
또한 저장된 결정 데이터를 바탕으로 사용자의 결정 패턴을 통계 형태로 확인할 수 있습니다.

## 주요 기능

* 결정 작성 및 저장
* 저장된 결정 목록 조회
* Activity 간 Intent를 활용한 데이터 전달
* ChatGPT API 기반 AI 분석 결과 제공
* 날씨 API 연동
* 결정 패턴 통계 및 대시보드 제공
* 타임캡슐 회고 작성
* 분석 결과 공유 기능

## 사용 기술

* Android
* Kotlin / Java
* Room Database
* RecyclerView
* Retrofit
* Coroutine
* ChatGPT API
* Weather API
* Intent

## 화면 구성

### Activity 1: 결정 작성 / 기록 관리

* 사용자가 결정 내용을 입력
* 입력값 검증 후 Room DB에 저장
* 저장된 결정 목록을 RecyclerView로 표시
* 다른 Activity로 데이터 전달

### Activity 2: AI 분석 결과

* 사용자의 결정 데이터를 기반으로 AI 분석 요청
* ChatGPT API 연동
* 날씨 API 연동
* Retrofit과 Coroutine을 사용한 비동기 API 처리
* 분석 결과 화면 출력

### Activity 3: 타임캡슐 회고

* 과거 결정에 대한 회고 작성
* 저장된 결정 데이터와 연결하여 회고 관리

### Activity 4: 패턴 분석 대시보드

* 저장된 결정 데이터를 기반으로 통계 계산
* 결정 패턴 분석 결과 시각화
* 분석 결과 공유 기능 제공

## 실행 방법

1. 프로젝트를 Android Studio에서 연다.
2. 필요한 API Key를 설정한다.
3. Gradle Sync를 실행한다.
4. 에뮬레이터 또는 Android 기기에서 앱을 실행한다.
