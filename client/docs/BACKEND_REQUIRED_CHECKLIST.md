# 백엔드 필수 점검 가이드 (로컬 기준)

## 1. 환경 변수
- `.env.example`를 기준으로 `.env` 구성
- `SWEETBOOK_API_BASE_URL`는 `/v1` 없이 설정
- 운영 DB는 MySQL 사용 시 `SPRING_PROFILES_ACTIVE=prod` 설정

## 2. 데이터베이스/Flyway
- 기본: H2 메모리 DB
- 운영 시뮬레이션: MySQL + `application-prod.yml`
- 실행 시 Flyway `V1__init_schema.sql` 적용 여부 확인

## 3. CSV 적재 정책
- 헤더 정규화(`ActivityCsvHeaderNormalizer`) 사용
- 중복 방지 키: `user_id + external_activity_id`
- 실패 행은 `skippedRows`로 반환하여 재처리 가능

## 4. 사진 저장소 정책
- 업로드 제한: `PHOTO_MAX_FILE_SIZE_BYTES`
- multipart 제한과 동일하게 유지
- 삭제는 DB 소유권 검증 후 수행

## 5. Sweetbook 연동 정책
- 모든 외부 호출 실패는 `SWEETBOOK_001`로 매핑
- idempotency key는 앨범 범위(`order-{albumId}-{externalRef}`)
- 연결/응답 타임아웃 환경변수로 관리

## 6. 주문/웹훅 상태머신
- 원격 상태코드: 20/25/30/40/50/60/70/80/81/90 매핑
- 역전이 방지
- 동일 delivery id 중복 처리 방지
- `order.restored`만 CANCELLED -> CREATED 복구 허용

## 7. Swagger 문서
- `/swagger-ui/index.html`에서 앨범/사진/주문 API 확인
- 컨트롤러 `@Operation` 문구와 실제 DTO 동기화 유지

## 8. 필수 테스트 실행
```bash
./gradlew.bat test --tests com.sweetbook.server.order.service.OrderServiceTest
./gradlew.bat test --tests com.sweetbook.server.order.service.OrderModificationServiceTest
./gradlew.bat test --tests com.sweetbook.server.order.service.OrderWebhookE2ETest
./gradlew.bat test --tests com.sweetbook.server.sweetbook.webhook.SweetbookWebhookControllerTest
./gradlew.bat test --tests com.sweetbook.server.activity.service.ActivityCsvImportServiceTest
./gradlew.bat test --tests com.sweetbook.server.photo.service.ActivityPhotoServiceTest
```

## 9. 문서-코드 동기화 원칙
- DTO 필드 변경 시 `docs/ALBUM_PHOTO_API_CONTRACT.md`, `docs/ORDER_API_CONTRACT.md` 즉시 반영
- 운영 체크 변경 시 `docs/OPS_CHECKLIST.md` 반영

## 10. 최종 확인
- `./gradlew.bat compileJava test` 통과
- 주요 API 수동 점검: 인증 -> 활동 -> 앨범 -> 사진 -> 책 -> 주문 -> 웹훅
