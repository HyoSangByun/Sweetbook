# AGENTS.md ??RunBook Refactoring Guide

## Instructions for the Agent

Before touching any code, read this entire file. Then read all Sweetbook API specs listed in
Section 1. Only after fully understanding the API contracts should you begin resolving the issues
in Section 2.

---

## Section 1 ??Sweetbook Book Print API Reference

> Base URLs
> - Sandbox : `https://api-sandbox.sweetbook.com/v1`
> - Live     : `https://api.sweetbook.com/v1`
>
> All requests require `Authorization: Bearer <API_KEY>` header.
> **API Key must only be used server-side. Never expose it to the client.**

---

### 1-1. Common Rules

**Authentication**
```
Authorization: Bearer SB{prefix}.{secret}
```

**Response envelope ??always present**
```json
{ "success": true,  "message": "...", "data": { ... } }
{ "success": false, "message": "...", "data": null, "errors": [], "fieldErrors": [] }
```

**Pagination** ??`limit` (default 20, max 100) / `offset` (default 0)

**Rate limits**
| Policy  | Scope          | Limit       |
|---------|----------------|-------------|
| auth    | auth endpoints | 10 req/min  |
| general | all APIs       | 300 req/min |
| upload  | file upload    | 200 req/min |

Exceeding limits ??`429 Too Many Requests`, check `Retry-After` header (60 s).

**Idempotency**
Send `Idempotency-Key: <uuid>` on `POST /books`, `POST /orders`.
Same key + same body ??returns cached response.
Same key + different body ??`409 Conflict`.

**HTTP error codes**
| Code | Meaning              |
|------|----------------------|
| 400  | Validation failure   |
| 401  | Invalid API key      |
| 402  | Insufficient credits |
| 403  | Forbidden            |
| 404  | Not found            |
| 409  | Idempotency conflict |
| 429  | Rate limit exceeded  |
| 500  | Server error         |

---

### 1-2. Books API

**Book status**: `0` = draft, `2` = finalized, `9` = deleted

#### Create book
```
POST /v1/books
Content-Type: application/json
Idempotency-Key: <uuid>   ??recommended

{
  "title": "string (1-255)",      // required
  "bookSpecUid": "SQUAREBOOK_HC", // required
  "specProfileUid": "sp_...",     // optional
  "externalRef": "string (??00)"  // optional
}
??201 { "data": { "bookUid": "bk_..." } }
```

#### List books
```
GET /v1/books?pdfStatusIn=1,2&createdFrom=2026-01-01&limit=10
??200 { "data": { "books": [...], "total", "limit", "offset" } }

Book fields: bookUid, title, bookSpecUid, status, pdfStatus, pdfRequestedAt, createdAt, externalRef
```

#### Add cover
```
POST /v1/books/{bookUid}/cover
Content-Type: multipart/form-data

Fields:
  templateUid   string  required
  parameters    string  optional  (JSON string of template params)
  <varName>     file    optional  (image file matching template variable name)

Image can be provided as:
  - direct file upload (multipart field)
  - URL string inside parameters JSON
  - server fileName from /photos upload
  - "$upload" placeholder in parameters to map to uploaded file

??201 inserted | 200 updated
```

#### Upload photo
```
POST /v1/books/{bookUid}/photos
Content-Type: multipart/form-data
Field: file (image, max 50 MB)

Supported: jpg, jpeg, png, gif, bmp, webp, heic, heif
Auto-convert: HEIC/HEIF?묳PG, GIF/WebP?뭁NG, BMP?묳PG
Resize: long-axis 4000px (original), 800px (thumbnail)
Duplicate check: MD5 hash ??returns isDuplicate:true + existing fileName if duplicate

??201 { "data": { "fileName": "photo....JPG", "isDuplicate": false, ... } }
```

#### Add content page
```
POST /v1/books/{bookUid}/contents?breakBefore=page|column|none
Content-Type: multipart/form-data

Fields:
  templateUid   string  required
  parameters    string  optional  (JSON string)
  <varName>     file    optional  (image files)

required:true param missing ??400
required:false param missing ??element removed from page silently

??201 { "data": { "result": "inserted", "breakBefore": "page", "pageCount": 4 } }
```

#### Reset content pages
```
DELETE /v1/books/{bookUid}/contents
??200 { "data": { "deletedPages": 15 } }
(cover is preserved; for dev/test use only)
```

#### Delete book
```
DELETE /v1/books/{bookUid}
??200 { "data": { "bookUid": "...", "status": 9 } }
(soft delete ??status becomes 9)
```

#### Finalize book
```
POST /v1/books/{bookUid}/finalization
??201 finalized | 200 already finalized (idempotent)
??400 if page count violates spec rules (min/max/increment)

After finalization: no more page edits allowed.
Spine width auto-adjusted based on final page count.
```

---

### 1-3. Orders API

**Order status codes**
| Code | Key                 |
|------|---------------------|
| 20   | PAID                |
| 30   | CONFIRMED           |
| 40   | IN_PRODUCTION       |
| 50   | PRODUCTION_COMPLETE |
| 60   | SHIPPED             |
| 70   | DELIVERED           |
| 80   | CANCELLED           |
| 81   | CANCELLED_REFUND    |

#### Create order
```
POST /v1/orders
Content-Type: application/json
Idempotency-Key: <uuid>   ??REQUIRED to prevent double-charge

{
  "items": [
    { "bookUid": "bk_...", "quantity": 1 }   // book must be FINALIZED
  ],
  "shipping": {
    "recipientName": "string (??00)",   // required
    "recipientPhone": "string (??0)",   // required
    "postalCode": "string (??0)",       // required
    "address1": "string (??00)",        // required
    "address2": "string (??00)",        // optional
    "memo": "string (??00)"             // optional
  },
  "externalRef": "string (??00)"        // optional
}
??201 order object (includes creditBalanceAfter)
??402 if insufficient credits
```

#### Estimate price
```
POST /v1/orders/estimate
{ "items": [{ "bookUid": "bk_...", "quantity": 1 }] }
??200 { "data": { productAmount, shippingFee, packagingFee, totalAmount,
                   creditBalance, creditSufficient, currency } }
```

#### List orders
```
GET /v1/orders?status=20&from=2026-01-01&to=2026-03-31&limit=20&offset=0
```

#### Get order detail
```
GET /v1/orders/{orderUid}
??same shape as POST /orders response minus creditBalanceAfter
```

#### Cancel order
```
POST /v1/orders/{orderUid}/cancel
{ "cancelReason": "string" }
??only PAID + NORMAL orders cancellable
??credits refunded immediately in full
??status becomes CANCELLED_REFUND (81)
```

#### Update shipping address
```
PATCH /v1/orders/{orderUid}/shipping
??allowed only in PAID ~ CONFIRMED states
??send only fields to change
Fields: recipientName, recipientPhone, postalCode, address1, address2, shippingMemo
```

---

### 1-4. Templates API

```
GET /v1/templates?bookSpecUid=SQUAREBOOK_HC&templateKind=cover|content&category=album
GET /v1/templates/{templateUid}   ??includes parameters, layout, layoutRules, thumbnails

GET /v1/template-categories
```

**templateKind**
- `cover`   ??used with `POST /books/{uid}/cover`
- `content` ??used with `POST /books/{uid}/contents`

Cover template ??content template. They cannot be swapped.

**Parameter binding types**
| Type       | Value                       |
|------------|-----------------------------|
| text       | plain string                |
| file       | image URL or uploaded fileName |
| rowGallery | array of fileNames          |

---

### 1-5. BookSpecs API

```
GET /v1/book-specs
GET /v1/book-specs/{bookSpecUid}
```

**Available specs**
| bookSpecUid    | Size (mm)    | Cover     | Binding   | Pages (min~max, step) |
|----------------|--------------|-----------|-----------|----------------------|
| SQUAREBOOK_HC  | 243 횞 248    | Hardcover | PUR       | 24 ~ 130, step 2     |
| LAYFLAT_HC     | 243 횞 248    | Hardcover | LAYFLAT   | 16 ~ 46,  step 2     |
| SLIMALBUM_HC   | 243 횞 248    | Hardcover | SLIMALBUM | 20 ~ 30,  step 2     |

**Price formula**
```
total = priceBase + ((pageCount - pageMin) / pageIncrement) * pricePerIncrement
```
Always confirm with `POST /orders/estimate` ??shipping + packaging fees apply.

---

### 1-6. Credits API

```
GET /v1/credits
??{ "data": { "balance": 100000, "currency": "KRW", "env": "test" } }
```

- Sandbox and Live credits are **completely separate**.
- **Sandbox credit charging is allowed via `POST /v1/credits/sandbox/charge` for this project.**
- Credits are deducted on `POST /orders` and refunded on cancel.
- Check balance with `GET /credits` before placing an order.

---

### 1-7. Webhooks API

```
PUT    /v1/webhooks/config    register / update webhook URL
GET    /v1/webhooks/config    get current config
DELETE /v1/webhooks/config    deactivate webhook
POST   /v1/webhooks/test      send test event
GET    /v1/webhooks/deliveries?status=FAILED&limit=10
```

**Request headers on each delivery**
```
X-Webhook-Event:    order.created
X-Webhook-Delivery: wh_d7e8f9a0b1c2
X-Webhook-Timestamp: 1709280000
X-Webhook-Signature: sha256=e3b0c44298fc1c14...
```

**Signature verification**
```
signPayload = "{timestamp}.{rawBody}"
expected    = "sha256=" + HMAC-SHA256(secretKey, signPayload).hex
verify      = timingSafeEqual(expected, X-Webhook-Signature)
```

**Retry policy**: up to 3 retries (1 min / 5 min / 30 min). After 3 failures ??`EXHAUSTED`.

**Deduplication**: use `X-Webhook-Delivery` to detect duplicates.

---

### 1-8. Webhook Events

All events share these common fields:
```json
{
  "event": "order.created",
  "orderUid": "or_8f3a2b1c",
  "bookUid": "bk_e4d5c6b7",
  "status": "PAID",
  "isTest": false,
  "timestamp": "2025-03-15T10:30:00Z"
}
```

**Event-specific fields**

| Event                 | Extra fields                                              |
|-----------------------|-----------------------------------------------------------|
| order.created         | quantity, totalCredits, shippingAddress                   |
| order.cancelled       | cancelledAt, cancelReason, refundedCredits                |
| order.restored        | restoredAt, deductedCredits                               |
| production.confirmed  | confirmedAt, estimatedShipDate                            |
| production.started    | startedAt                                                 |
| production.completed  | completedAt                                               |
| shipping.departed     | trackingNumber, trackingCarrier, shippedAt                |
| shipping.delivered    | deliveredAt                                               |

**Sample payloads**
```json
// order.created
{
  "event": "order.created", "orderUid": "or_8f3a2b1c", "bookUid": "bk_e4d5c6b7",
  "status": "PAID", "quantity": 2, "totalCredits": 35000,
  "shippingAddress": { "recipientName": "?띻만??, "phone": "010-1234-5678",
    "zipCode": "06234", "address1": "?쒖슱?밸퀎??媛뺣궓援??뚰뿤?濡?123", "address2": "4痢?401?? },
  "isTest": false, "timestamp": "2025-03-15T10:30:00Z"
}
// order.cancelled
{
  "event": "order.cancelled", "orderUid": "or_8f3a2b1c", "bookUid": "bk_e4d5c6b7",
  "status": "CANCELLED", "cancelledAt": "2025-03-16T11:20:00Z",
  "cancelReason": "怨좉컼 ?붿껌???섑븳 痍⑥냼", "refundedCredits": 35000,
  "isTest": false, "timestamp": "2025-03-16T11:20:00Z"
}
// production.confirmed
{
  "event": "production.confirmed", "orderUid": "or_8f3a2b1c", "bookUid": "bk_e4d5c6b7",
  "status": "CONFIRMED", "confirmedAt": "2025-03-15T14:00:00Z",
  "estimatedShipDate": "2025-03-20", "isTest": false, "timestamp": "2025-03-15T14:00:00Z"
}
// shipping.departed
{
  "event": "shipping.departed", "orderUid": "or_8f3a2b1c", "bookUid": "bk_e4d5c6b7",
  "status": "SHIPPED", "trackingNumber": "1234567890123", "trackingCarrier": "CJ",
  "shippedAt": "2025-03-20T16:45:00Z", "isTest": false, "timestamp": "2025-03-20T16:45:00Z"
}
// shipping.delivered
{
  "event": "shipping.delivered", "orderUid": "or_8f3a2b1c", "bookUid": "bk_e4d5c6b7",
  "status": "DELIVERED", "deliveredAt": "2025-03-22T14:00:00Z",
  "isTest": false, "timestamp": "2025-03-22T14:00:00Z"
}
```

---

## Section 2 ??Issues to Resolve

Resolve **all** issues below. Do not skip any. For each issue, understand the root cause first,
then fix it. Do not introduce regressions.

---

### ISSUE-01 쨌 `Cannot read properties of null (reading 'accessToken')` on sign-up UI

**Symptom**: The error is visible in the sign-up screen.
**Fix**: Guard against null before accessing `accessToken`. Trace where the auth/session object
is read and add a null-check or optional-chaining (`?.`) before accessing `.accessToken`.
If the value is legitimately null at that point (e.g., user not yet authenticated), handle that
state gracefully in the UI instead of crashing.

---

### ISSUE-02 쨌 Toggle left of "Import CSV" button is UX-awkward when there are no activities

**Symptom**: On first login with no activity history, a toggle appears to the left of the
CSV import button, which is confusing.
**Fix**: Hide the toggle when there is no activity data. The toggle should only be shown when
there is existing data to toggle between views. Show only the CSV import CTA on an empty state.

---

### ISSUE-03 쨌 Broken image display after selecting activities and attaching photos

**Symptom**: After a user selects activities and attaches a photo, the image renders broken in
the UI.
**Fix**: Ensure the image preview uses a valid object URL (`URL.createObjectURL(file)`) or a
proper `src` binding. Check that the `<img>` tag's `src` is not set to a raw `File` object.
Revoke old object URLs before reassigning to avoid memory leaks.

---

### ISSUE-04 쨌 Image file is uploaded to source code when attaching a photo

**Symptom**: When a user attaches a photo during activity selection, the image file ends up
committed or stored in the source tree.
**Fix**: Images must only be held in memory (browser `File` / `Blob` objects) until the user
confirms book creation. At that point, upload them to Sweetbook via
`POST /v1/books/{bookUid}/photos` and use the returned `fileName`. Never write image binaries
to the local filesystem or include them in version-controlled assets.

---

### ISSUE-05 쨌 Book creation flow must expose user-facing inputs

**Symptom**: Users cannot set book title, book spec (?먰삎), cover template, content template,
or content (photos optional) before creation.
**Fix**: Add a book creation form with the following fields:

| Field            | Source                              | Required |
|------------------|-------------------------------------|----------|
| Book title       | free text input                     | yes      |
| BookSpec (?먰삎)   | `GET /v1/book-specs` ??dropdown     | yes      |
| Cover template   | `GET /v1/templates?templateKind=cover&bookSpecUid=<selected>` ??dropdown | yes |
| Content template | `GET /v1/templates?templateKind=content&bookSpecUid=<selected>` ??dropdown | yes |
| Activities       | selected dates from running data    | yes      |
| Photos per date  | optional file upload per activity   | no       |

When bookSpecUid changes, re-fetch templates filtered to that spec. The selected cover/content
templateUid must be passed to `POST /v1/books/{uid}/cover` and `POST /v1/books/{uid}/contents`.

---

### ISSUE-06 쨌 No book preview before payment

**Symptom**: There is no preview step before the user proceeds to checkout.
**Fix**: The Sweetbook API does not provide a preview endpoint. Implement a **client-side
preview summary screen** that displays:
- Book title
- Selected BookSpec name
- Cover template thumbnail (use `thumbnails.layout` from template detail)
- Content template thumbnail
- List of selected activity dates with attached photo previews (object URLs)
- Estimated page count
- Price from `POST /v1/orders/estimate`

Show this screen after all inputs are filled and before calling
`POST /v1/books/{uid}/finalization`. Only proceed to finalization + order when the user
explicitly confirms.

---

### ISSUE-07 쨌 Credit balance query is broken

**Symptom**: The credit balance feature does not work.
**Fix**: Implement `GET /v1/credits` on the backend and expose it via an internal API route
(e.g., `GET /api/credits`). The frontend should call this endpoint and display:
- Current balance (formatted as KRW)
- Environment label (Sandbox / Live)

Do **not** call the Sweetbook API directly from the frontend. The API key must stay server-side.

---

### ISSUE-08 쨌 Idempotency Key for credit charge must be generated server-side

**Symptom**: The frontend currently sends the `Idempotency-Key` for credit-related requests,
which is wrong.
**Fix**: The backend must generate a UUID v4 `Idempotency-Key` internally for every
mutating request to Sweetbook (`POST /orders`, `POST /books`, etc.).
The frontend must **never** supply or even see the `Idempotency-Key`.
Use `UUID.randomUUID().toString()` (Java) and attach it as a request header in the
`SweetbookService` layer before forwarding to the Sweetbook API.

---

### ISSUE-09 쨌 Credit transaction history ??do NOT implement

Credit transaction history (`GET /v1/credits/transactions` or similar) is **out of scope**.
Do not implement it. If there is any existing stub or placeholder UI for it, remove it entirely.

---

## Section 3 ??Constraints

1. **API Key security**: the Sweetbook API Key must only exist in backend environment variables.
   It must never appear in frontend code, browser network requests, or be committed to git.
2. **Idempotency**: every `POST /orders` and `POST /books` call from `SweetbookService` must
   attach a freshly generated `Idempotency-Key` header.
3. **No preview API**: Sweetbook does not offer a PDF/preview endpoint. Do not attempt to call
   one. Implement preview as a client-side summary only (see ISSUE-06).
4. **Credits charging**: for this project, sandbox charge API (`POST /v1/credits/sandbox/charge`) may be used from backend and exposed to UI via internal API.
5. **Out of scope**: credit transaction history (ISSUE-09). Do not implement.
6. **Template?밄ookSpec coupling**: always filter templates by `bookSpecUid`. Never show a
   template that belongs to a different spec.

