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

### 2) 앨범 상세 조회
- `GET /api/albums/{albumId}`

### 3) 앨범 수정
- `PATCH /api/albums/{albumId}`

### 4) 앨범 활동 선택
- `POST /api/albums/{albumId}/activities`

### 5) 앨범 활동 선택 해제
- `DELETE /api/albums/{albumId}/activities/{activityId}`

## 사진 API

### 6) 사진 업로드
- `POST /api/albums/{albumId}/activities/{activityId}/photos`
- Content-Type: `multipart/form-data`
- Form field: `file`

### 7) 사진 목록 조회
- `GET /api/albums/{albumId}/activities/{activityId}/photos`

### 8) 사진 삭제
- `DELETE /api/albums/{albumId}/activities/{activityId}/photos/{photoId}`

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
