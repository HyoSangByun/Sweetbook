# 운영 전 체크리스트

## 1) 환경 변수

- [ ] `SPRING_PROFILES_ACTIVE=prod` 적용
- [ ] `JWT_SECRET` 운영용 시크릿(32바이트 이상) 적용
- [ ] `SWEETBOOK_SANDBOX_API_KEY` 또는 Live API 키 적용
- [ ] `SWEETBOOK_BOOK_SPEC_UID` 운영 스펙 UID 적용
- [ ] `SWEETBOOK_WEBHOOK_SECRET` 설정 완료
- [ ] `SWEETBOOK_WEBHOOK_TIMESTAMP_TOLERANCE` 설정 확인(예: `5m`)
- [ ] `PHOTO_STORAGE_DIR` 운영 경로 권한 확인
- [ ] `PHOTO_MAX_FILE_SIZE_BYTES`와 multipart 설정 일치 확인

## 2) 데이터베이스 (MySQL)

- [ ] 운영 DB를 MySQL로 구성 (`application-prod.yml`)
- [ ] `MYSQL_URL`, `MYSQL_USERNAME`, `MYSQL_PASSWORD` 설정
- [ ] Flyway 마이그레이션 정상 실행(`V1__init_schema.sql`)
- [ ] `spring.jpa.hibernate.ddl-auto=validate` 유지 확인
- [ ] 운영 DB 백업/복구 절차 점검
- [ ] 인덱스 생성 여부 확인
  - `idx_activities_user_month`
  - `idx_activity_photos_album_activity`
  - `idx_album_project_created_at`

## 3) 보안

- [ ] Swagger 운영 접근 정책 확인
- [ ] Webhook 엔드포인트는 JWT 제외, 서명 검증 필수 적용 확인
- [ ] Webhook timestamp 허용 오차 검증 동작 확인
- [ ] `X-Webhook-Signature` 검증 실패 시 401 반환 확인

## 4) 주문/웹훅

- [ ] `/api/webhooks/sweetbook/orders` HTTPS 외부 노출 확인
- [ ] Sweetbook 콘솔에 webhook URL 등록
- [ ] Sweetbook `POST /v1/webhooks/test`로 수신 검증
- [ ] 같은 `X-Webhook-Delivery` 재전송 시 중복 처리 방지 확인
- [ ] 상태 역전이(예: 40 -> 25) 무시 동작 확인
- [ ] `order.restored` 이벤트 시 취소 상태 복구 정책 확인

## 5) 모니터링/로깅

- [ ] 주문 생성 실패(`SWEETBOOK_001`) 로그 수집 확인
- [ ] webhook 서명/타임스탬프 실패 로그 모니터링 추가
- [ ] 알람 기준(5xx 비율, 주문 실패율, webhook 실패율) 설정

## 6) 테스트

- [ ] `OrderServiceTest` 통과
- [ ] `OrderModificationServiceTest` 통과
- [ ] `OrderWebhookE2ETest` 통과
- [ ] `SweetbookWebhookControllerTest` 통과
- [ ] 핵심 시나리오 수동 점검
  - 앨범 생성 -> 책 생성 -> 주문 생성
  - 주문 취소
  - 배송지 변경
  - webhook 상태 반영(제작/발송/배송/취소)

