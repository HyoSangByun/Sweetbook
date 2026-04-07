# 운영 체크리스트

## 1) 환경 변수

- [ ] `SPRING_PROFILES_ACTIVE=prod`
- [ ] `JWT_SECRET` 운영 시크릿(32바이트 이상)
- [ ] `SWEETBOOK_SANDBOX_API_KEY` 또는 Live API Key
- [ ] `SWEETBOOK_BOOK_SPEC_UID`
- [ ] `SWEETBOOK_WEBHOOK_SECRET`
- [ ] `SWEETBOOK_WEBHOOK_TIMESTAMP_TOLERANCE` (예: `PT5M`)
- [ ] `PHOTO_STORAGE_DIR` 권한 확인
- [ ] `PHOTO_MAX_FILE_SIZE_BYTES`와 multipart 제한값 일치

## 2) 데이터베이스 (MySQL)

- [ ] `application-prod.yml` 기준 MySQL 연결 확인
- [ ] `MYSQL_URL`, `MYSQL_USERNAME`, `MYSQL_PASSWORD` 설정
- [ ] Flyway 마이그레이션 정상 수행 (`V1__init_schema.sql`)
- [ ] `spring.jpa.hibernate.ddl-auto=validate` 확인
- [ ] 주요 인덱스 생성 확인
  - `idx_activities_user_month`
  - `idx_activity_photos_album_activity`
  - `idx_album_project_created_at`

## 3) 보안

- [ ] Swagger 운영 노출 정책 확인
- [ ] Webhook 경로 JWT 제외 + 서명 검증 적용 확인
- [ ] Webhook timestamp 허용 오차 검증 동작 확인
- [ ] `X-Webhook-Signature` 검증 실패 시 401 확인

## 4) 주문/Webhook

- [ ] Sweetbook 콘솔에 Webhook URL 등록
- [ ] `POST /v1/webhooks/test` 수신 검증
- [ ] `X-Webhook-Delivery` 중복 방지 확인
- [ ] 역전이 이벤트 무시 동작 확인
- [ ] `order.restored` 복구 전이 확인

## 5) 모니터링/로그

- [ ] `SWEETBOOK_001` 오류 로그 수집
- [ ] Webhook 서명/타임스탬프 실패 로그 수집
- [ ] 주문 실패율/웹훅 실패율 모니터링

## 6) 테스트

- [ ] `OrderServiceTest`
- [ ] `OrderModificationServiceTest`
- [ ] `OrderWebhookE2ETest`
- [ ] `SweetbookWebhookControllerTest`
