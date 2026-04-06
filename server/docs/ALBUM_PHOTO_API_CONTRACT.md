# 앨범/사진 API 계약서 (연동 고정)

이 문서는 현재 백엔드 구현 기준의 앨범/사진 API 계약을 고정하기 위한 문서다.

## 공통 규칙

- Base Path: `/api`
- 인증: `Authorization: Bearer {accessToken}` 필수
- 응답 포맷:

```json
{
  "success": true,
  "data": {},
  "error": null,
  "timestamp": "2026-04-03T16:00:00+09:00"
}
```

- 실패 포맷:

```json
{
  "success": false,
  "data": null,
  "error": {
    "code": "ALBUM_001",
    "message": "오류 메시지",
    "details": null
  },
  "timestamp": "2026-04-03T16:00:00+09:00"
}
```

## 앨범 API

### 1) 앨범 생성

- `POST /api/albums`
- Request JSON:

```json
{
  "month": "2026-04",
  "title": "4월 운동 기록",
  "subtitle": "지속 가능한 루틴",
  "monthlyReview": "주 4회 이상 달성"
}
```

- Response `data`:
  - `albumId`
  - `month`
  - `title`
  - `subtitle`
  - `monthlyReview`
  - `status` (`DRAFT`)
  - `hasPhoto` (boolean)
  - `selectedActivityCount`
  - `selectedActivities` (배열)
  - `createdAt`, `updatedAt`

### 2) 앨범 상세 조회

- `GET /api/albums/{albumId}`
- Response `data`: 앨범 생성 응답과 동일 구조

### 3) 앨범 수정

- `PATCH /api/albums/{albumId}`
- Request JSON (부분 수정):

```json
{
  "title": "수정 제목",
  "subtitle": "수정 부제",
  "monthlyReview": "수정 회고"
}
```

### 4) 앨범 활동 선택

- `POST /api/albums/{albumId}/activities`
- Request JSON:

```json
{
  "activityIds": [101, 102, 103]
}
```

- Response `data`:
  - `addedCount`
  - `skippedCount`
  - `selectedActivityCount`

### 5) 앨범 활동 선택 해제

- `DELETE /api/albums/{albumId}/activities/{activityId}`
- Response `data`:
  - `deleted` (true)
  - `selectedActivityCount`

## 사진 API

### 6) 사진 업로드

- `POST /api/albums/{albumId}/activities/{activityId}/photos`
- Content-Type: `multipart/form-data`
- Form field: `file` (이미지 파일)
- Response `data`:
  - `photoId`
  - `originalFileName`
  - `contentType`
  - `fileSize`

### 7) 사진 목록 조회

- `GET /api/albums/{albumId}/activities/{activityId}/photos`
- Response `data`: 배열
  - `photoId`
  - `originalFileName`
  - `contentType`
  - `fileSize`
  - `createdAt`

### 8) 사진 삭제

- `DELETE /api/albums/{albumId}/activities/{activityId}/photos/{photoId}`
- Response `data`:
  - `deleted` (true)

## 주요 에러 코드(연동 시 참고)

- `AUTH_001`: 인증 필요
- `AUTH_002`: 권한 없음
- `ALBUM_001`: 앨범 없음
- `ALBUM_002`: 앨범에서 선택된 활동 없음
- `ACTIVITY_001`: 활동 없음
- `PHOTO_001`: 사진 없음
- `COMMON_001`: 잘못된 요청
- `FILE_001`: 파일 처리 오류
- `SWEETBOOK_001`: Sweetbook API 호출 실패

## 프론트 연동 체크리스트

- [ ] 모든 앨범/사진 API 요청에 `Authorization: Bearer ...` 포함
- [ ] 사진 업로드 요청의 multipart 필드명이 정확히 `file`인지 확인
- [ ] 상세 조회 응답의 `hasPhoto` 값을 책 생성 버튼 분기/표시에 활용
- [ ] 활동 선택 후 즉시 `selectedActivityCount` 반영
- [ ] 선택 해제 후 목록/카운트 동기화
- [ ] 사진 삭제 후 목록 재조회 또는 로컬 상태 제거
- [ ] 에러 시 `error.code` 기준 사용자 메시지 매핑

## E2E 시나리오(수동 점검 순서)

1. 앨범 생성
2. 앨범 활동 선택
3. 사진 업로드
4. 사진 목록 조회
5. 앨범 상세 조회에서 `hasPhoto=true` 확인
6. 사진 삭제
7. 앨범 활동 선택 해제
8. 앨범 상세 조회에서 `selectedActivityCount`/목록 최종 확인
