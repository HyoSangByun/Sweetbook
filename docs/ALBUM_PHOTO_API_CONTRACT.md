# 앨범/사진 API 계약서 (연동 고정)

이 문서는 현재 백엔드 구현 기준의 앨범/사진 API 계약을 고정하기 위한 문서입니다.

## 공통 규칙

- Base Path: `/api`
- 인증: `Authorization: Bearer {accessToken}` 필수
- 성공 응답 형식

```json
{
  "success": true,
  "data": {},
  "error": null,
  "timestamp": "2026-04-06T12:00:00+09:00"
}
```

- 실패 응답 형식

```json
{
  "success": false,
  "data": null,
  "error": {
    "code": "ALBUM_001",
    "message": "오류 메시지",
    "details": null
  },
  "timestamp": "2026-04-06T12:00:00+09:00"
}
```

## 앨범 API

### 1) 앨범 생성
- `POST /api/albums`
- Response `data`: `AlbumResponse`
  - `albumId` (`number`): 앨범 ID
  - `month` (`string`, `YYYY-MM`): 대상 월
  - `title` (`string`): 앨범 제목
  - `subtitle` (`string | null`): 부제
  - `monthlyReview` (`string | null`): 월간 회고
  - `status` (`string`): 앨범 상태
  - `bookUid` (`string | null`): 생성된 책 UID
  - `bookStatus` (`string`): 책 생성 상태
  - `bookGeneratedAt` (`string(datetime) | null`): 책 생성 완료 시각
  - `hasPhoto` (`boolean`): 선택된 활동 중 사진 존재 여부
  - `selectedActivityCount` (`number`): 선택된 활동 수
  - `selectedActivities`: 선택된 활동 목록(`AlbumActivityItemResponse`)
    - `albumActivityId`: 앨범 활동 ID
    - `activityId`: 원본 활동 ID
    - `externalActivityId`: 외부 활동 식별자
    - `activityDateTime`: 활동 일시
    - `activityType`: 활동 타입
    - `activityName`: 활동명
    - `distanceKm`: 거리(km)
    - `movingTimeSeconds`: 이동 시간(초)
    - `memo`: 메모
  - `createdAt`: 생성 시각
  - `updatedAt`: 수정 시각
- 예시 응답

```json
{
  "success": true,
  "data": {
    "albumId": 12,
    "month": "2026-04",
    "title": "4월 운동 기록",
    "subtitle": "지구력 강화 루틴",
    "monthlyReview": "주 4회 이상 달성",
    "status": "DRAFT",
    "bookUid": null,
    "bookStatus": "NOT_REQUESTED",
    "bookGeneratedAt": null,
    "hasPhoto": true,
    "selectedActivityCount": 2,
    "selectedActivities": [
      {
        "albumActivityId": 2001,
        "activityId": 101,
        "externalActivityId": "strava_101",
        "activityDateTime": "2026-04-03T19:20:00",
        "activityType": "RUN",
        "activityName": "Evening Run",
        "distanceKm": 7.2,
        "movingTimeSeconds": 2580,
        "memo": "페이스 안정적으로 유지"
      }
    ],
    "createdAt": "2026-04-06T12:10:00",
    "updatedAt": "2026-04-06T12:20:00"
  },
  "error": null,
  "timestamp": "2026-04-06T12:20:00+09:00"
}
```

### 2) 앨범 상세 조회
- `GET /api/albums/{albumId}`
- Response `data`: `AlbumResponse` (필드 동일)
  - `hasPhoto` (`boolean`): 프론트에서 책 템플릿 분기/미리보기 분기에 사용하는 대표 필드
- 예시 응답: 앨범 생성 응답과 동일 구조

### 3) 앨범 수정
- `PATCH /api/albums/{albumId}`

### 4) 앨범 활동 선택
- `POST /api/albums/{albumId}/activities`
- Response `data`: `SelectAlbumActivitiesResponse`
  - `addedCount` (`number`): 이번 요청으로 추가된 활동 수
  - `skippedCount` (`number`): 이미 선택되어 건너뛴 활동 수
  - `selectedActivityCount` (`number`): 요청 반영 후 최종 선택 활동 수
- 예시 응답

```json
{
  "success": true,
  "data": {
    "addedCount": 3,
    "skippedCount": 1,
    "selectedActivityCount": 8
  },
  "error": null,
  "timestamp": "2026-04-06T12:25:00+09:00"
}
```

### 5) 앨범 활동 선택 해제
- `DELETE /api/albums/{albumId}/activities/{activityId}`
- Response `data`: `DeselectAlbumActivityResponse`
  - `deleted` (`boolean`): 삭제 성공 여부
  - `selectedActivityCount` (`number`): 요청 반영 후 최종 선택 활동 수
- 예시 응답

```json
{
  "success": true,
  "data": {
    "deleted": true,
    "selectedActivityCount": 7
  },
  "error": null,
  "timestamp": "2026-04-06T12:26:00+09:00"
}
```

## 사진 API

### 6) 사진 업로드
- `POST /api/albums/{albumId}/activities/{activityId}/photos`
- Content-Type: `multipart/form-data`
- Form field: `file`
- HTTP Status: `200 OK`
- Response `data`: `ActivityPhotoUploadResponse`
  - `photoId`: 사진 ID
  - `originalFileName`: 원본 파일명
  - `contentType`: MIME 타입
  - `fileSize`: 파일 크기(byte)
  - 참고: 현재 업로드 응답 DTO에는 `createdAt` 필드가 없고, 생성 시각은 목록 조회 응답에서 확인
- 예시 응답

```json
{
  "success": true,
  "data": {
    "photoId": 301,
    "originalFileName": "run-2026-04-03.jpg",
    "contentType": "image/jpeg",
    "fileSize": 824512
  },
  "error": null,
  "timestamp": "2026-04-06T13:10:00+09:00"
}
```

### 7) 사진 목록 조회
- `GET /api/albums/{albumId}/activities/{activityId}/photos`
- HTTP Status: `200 OK`
- Response `data`: `ActivityPhotoItemResponse[]`
  - `photoId`: 사진 ID
  - `originalFileName`: 원본 파일명
  - `contentType`: MIME 타입
  - `fileSize`: 파일 크기(byte)
  - `createdAt`: 업로드 시각
- 예시 응답

```json
{
  "success": true,
  "data": [
    {
      "photoId": 301,
      "originalFileName": "run-2026-04-03.jpg",
      "contentType": "image/jpeg",
      "fileSize": 824512,
      "createdAt": "2026-04-06T13:10:00"
    },
    {
      "photoId": 302,
      "originalFileName": "run-2026-04-03-2.jpg",
      "contentType": "image/jpeg",
      "fileSize": 702114,
      "createdAt": "2026-04-06T13:11:12"
    }
  ],
  "error": null,
  "timestamp": "2026-04-06T13:11:20+09:00"
}
```

### 8) 사진 삭제
- `DELETE /api/albums/{albumId}/activities/{activityId}/photos/{photoId}`
- HTTP Status: `200 OK`
- Response `data`: `ActivityPhotoDeleteResponse`
  - `deleted` (`boolean`): 삭제 성공 여부
- 사이드이펙트
  - 삭제 성공 후 동일 활동의 사진 목록 조회 결과에서 해당 `photoId`는 제거되어야 함
  - 앨범 상세의 `hasPhoto`는 서버 계산값이므로, 필요 시 앨범 상세 재조회로 동기화
- 예시 응답

```json
{
  "success": true,
  "data": {
    "deleted": true
  },
  "error": null,
  "timestamp": "2026-04-06T13:12:00+09:00"
}
```

## 주요 에러 코드

- `AUTH_001`: 인증 필요
- `AUTH_002`: 권한 없음
- `ALBUM_001`: 앨범 없음
- `ALBUM_002`: 앨범에서 선택된 활동 없음
- `ACTIVITY_001`: 활동 없음
- `PHOTO_001`: 사진 없음
- `COMMON_001`: 잘못된 요청
- `FILE_001`: 파일 처리 오류
- `SWEETBOOK_001`: Sweetbook API 호출 실패

## 연동 체크리스트

- [ ] 모든 앨범/사진 API 요청에 `Authorization: Bearer ...` 포함
- [ ] 사진 업로드 시 multipart 필드명이 `file`인지 확인
- [ ] 앨범 상세 응답의 `hasPhoto`를 UI 분기값으로 사용
- [ ] 활동 선택/해제 시 `selectedActivityCount` 즉시 반영
- [ ] 사진 삭제 후 목록과 카운트 동기화 확인
