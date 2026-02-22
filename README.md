# prography-11th-backend

프로그라피 11기 백엔드 과제 구현 저장소입니다.

## 기술 스택
- Java 21
- Spring Boot 3.3.5
- Spring Data JPA
- H2 Database (in-memory)
- Gradle

## 실행 방법 (필수)
1. JDK 21을 설치합니다.
2. 아래 명령어로 서버를 실행합니다.

```bash
./gradlew bootRun
```

기본 포트는 `8080`입니다.

## 테스트 실행

```bash
./gradlew test
```

## 시드 데이터
서버 시작 시 자동으로 아래 데이터가 적재됩니다.
- Cohort: 10기, 11기
- Part: 각 Cohort별 SERVER, WEB, IOS, ANDROID, DESIGN
- Team: 11기 Team A, Team B, Team C
- 관리자 계정
  - `loginId`: `admin`
  - `password`: `admin1234`
  - `role`: `ADMIN`
- 관리자 초기 보증금: 100,000원

## 문서
- ERD: `/docs/ERD.md`
- System Design Architecture: `/docs/ARCHITECTURE.md`
- 구현 고민/정책 정리: `/docs/THOUGHTS.md`
- API 체크리스트: `/docs/API_CHECKLIST.md`
