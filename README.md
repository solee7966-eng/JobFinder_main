# JobFinder MSA Project (Public Portfolio)

## 프로젝트 소개

본 프로젝트는 채용 플랫폼을 주제로 한 **MSA(Microservice Architecture) 기반 웹 서비스**입니다.
사용자, 기업, 채용공고, 결제 등의 기능을 서비스 단위로 분리하여 설계하였으며,
Spring Boot, Eureka, Gateway를 활용한 마이크로서비스 구조를 적용했습니다.

※ 본 저장소는 **포트폴리오 공개용으로 재구성된 버전**이며,
실제 운영 환경의 민감한 정보(DB, API Key 등)는 제거되어 있습니다.

---

## 시스템 아키텍처

* MSA 구조 기반 서비스 분리
* API Gateway를 통한 라우팅 처리
* Eureka를 통한 서비스 디스커버리
* 서비스 간 통신 구조 구성

---

## 기술 스택

* **Backend**: Java, Spring Boot
* **Database**: Oracle
* **ORM / Mapper**: MyBatis
* **Infra**: Docker, AWS (EC2)
* **MSA**: Spring Cloud (Eureka, Gateway)
* **Build Tool**: Gradle

---

## 실행 방법

### 1. 환경 변수 설정

프로젝트 실행을 위해 아래 환경변수를 설정해야 합니다.
실제 값은 보안상 제거되었으며, 아래는 예시입니다.

```bash
# Common
JWT_SECRET=your_jwt_secret
SOLAPI_API_KEY=your_solapi_api_key
SOLAPI_API_SECRET=your_solapi_api_secret
SOLAPI_FROM_PHONE=01012345678
SOLAPI_BASE_URL=https://api.solapi.com
PORTONE_IMP_CODE=your_imp_code
PORTONE_API_KEY=your_portone_api_key
PORTONE_API_SECRET=your_portone_api_secret

# Database
DB_URL=jdbc:oracle:thin:@localhost:1521:xe
DB_USERNAME=your_db_username
DB_PASSWORD=your_db_password

# Eureka
EUREKA_DEFAULT_ZONE=http://localhost:8761/eureka

# File paths (prod example)
FILE_UPLOAD_DIR=/app/data/file_upload
FILE_PHOTOUPLOAD_DIR=/app/data/file_photoupload
FILE_EMAILATTACHFILE_DIR=/app/data/file_emailattachfile
FILE_IMAGES_DIR=/app/data/file_images
```

---

### 2. 실행 순서

MSA 구조이므로 아래 순서로 실행해야 합니다.

1. **Discovery Service 실행**
2. **Gateway Service 실행**
3. **Main / Board Service 실행**

---

## 프로젝트 구조

```
jobfinder-msa-portfolio/
 ┣ main-service
 ┣ board-service
 ┣ gateway-service
 ┣ discovery-service
```

---

## 보안 및 참고 사항

* 본 프로젝트는 **포트폴리오 공개용**으로 민감 정보는 제거되었습니다.
* 실제 운영 환경에서는 환경변수 또는 별도 설정 파일을 통해 관리합니다.
* 일부 설정은 예시값으로 대체되어 있으며, 실행 환경에 맞게 수정이 필요합니다.

---

## 주요 구현 포인트

* MSA 구조 설계 및 서비스 분리
* API Gateway 기반 라우팅 처리
* Eureka 기반 서비스 등록 및 탐색
* 결제 API 연동 및 포인트 시스템 구현
* 파일 업로드 및 관리 기능 구현

---

## 기타

* 포트폴리오 정리: 
* 배포 URL: jobfinder-antaehoon.kro.kr

---
