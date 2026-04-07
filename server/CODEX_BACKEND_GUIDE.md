# CODEX_BACKEND_GUIDE.md

## Role

You are the **backend implementation AI** for this project.  
The backend stack uses **Spring Boot**.  
You must **always explain and respond in Korean**.

---

## Project Goal

This service is a web application where users can view their workout records by month,  
select a specific month, choose which workout dates to include in a book,  
attach images only to selected dates if needed,  
and then **generate and order a monthly workout album book**.

The backend is responsible for:

- Email-based signup / login
- Storing workout data separately for each user
- Importing and querying CSV-based workout data
- Providing monthly workout statistics
- Creating / updating album drafts
- Managing selected workout dates
- Managing image uploads for selected workout dates
- Integrating with the Sweetbook Books API
- Integrating with the Sweetbook Orders API

---

## Documentation Location

- Backend/API contract docs are managed in the repository root `docs/` directory.
- If backend work is performed under `server/`, reference docs via `../docs/...`.

---

## Response Language Rules

- All explanations, comments, work summaries, and draft commit messages must be written in **Korean**.
- Even if the user asks in English, the default response language must be **Korean**.
- Code identifiers, class names, and method names should follow standard Java/Spring conventions.
- README files and API descriptions should also default to **Korean** unless explicitly requested otherwise.

---

## Absolute Principles

1. Always understand the current project structure before implementing anything.
2. Do not change too many files at once.
3. Keep implementation consistent with the existing design intent.
4. Do not invent Sweetbook API field names by guessing.
5. Separate external API integration code into a client layer.
6. Do not place business logic inside controllers.
7. Handle exceptions in a common and consistent way.
8. Never hardcode sensitive information in code.
9. Separate response DTOs from external API DTOs.
10. Do not perform large-scale refactoring unless explicitly requested.

---

## Backend Tech Stack

- Java 17 (follow current project toolchain)
- Spring Boot
- Spring Web
- Spring Security
- JWT-based authentication
- Spring Validation
- JPA or MyBatis depending on the current project setup; if the stack is already chosen, follow it
- H2 or MySQL
- Multipart file upload
- Sweetbook API calls via RestClient or WebClient

If the project already uses a specific technology, do not force a new stack into it. Follow the existing stack first.

---

## Implementation Scope

### 1. Authentication

- Email signup
- Email login
- JWT access token issuance
- Authenticated user lookup

### 2. Workout Data

- CSV-based import
- Query workout data by user
- Query available months
- Query workout list by month
- Query workout detail
- Query monthly statistics

### 3. Album

- Create monthly album draft
- Update title, subtitle, and monthly review
- Select workout dates to include in the book
- Save memo for each selected workout
- Upload / delete photos for selected workouts

### 4. Book Generation

- Integrate Sweetbook Books API
- Create cover
- Create month-start page
- Create content pages for selected workout dates
- Finalize the book

### 5. Order

- Receive shipping information
- Integrate Sweetbook Orders API
- Save order status

---

## Core Domains

Implement based on the following domains:

- User
- Activity
- AlbumProject
- AlbumActivity
- ActivityPhoto
- Order

### Relationship Principles

- User 1:N Activity
- User 1:N AlbumProject
- AlbumProject 1:N AlbumActivity
- Activity 1:N AlbumActivity
- AlbumActivity 1:N ActivityPhoto
- AlbumProject 1:N Order

Photos must belong to **AlbumActivity**, not the original Activity.  
This is because the same workout record may have photos in one album and no photos in another.

---

## Sweetbook API Integration Rules

- Use direct HTTP calls from the backend instead of the official SDK.
- Manage the Base URL through environment variables.
- Use the authentication header format: `Authorization: Bearer {API_KEY}`
- The required APIs are the Books API and Orders API.
- The book generation flow must follow this order:
  1. Create book
  2. Add cover
  3. Add month-start page
  4. Add content pages for selected workout dates
  5. Finalize
  6. Create order
- Use fixed template UIDs defined by the project.
- Check the actual Sweetbook API field names and parameter structure from the documentation before implementing. Never guess them.

---

## Fixed Template Rules

Do not make template selection overly dynamic. Use the following fixed rules.

### Book with Images

- Cover template: `40nimglmWLSh`
- Month-start template: `7kV0VVvWlwNI`
- Content template: `1XtN1225R7wN`

### Book without Images

- Cover template: `40nimglmWLSh`
- Month-start template: `7kV0VVvWlwNI`
- Content template: `5ZpsyEJW5PZW`

### Branching Rule

- If at least one selected workout date in the album has a photo, treat it as a **book with images**
- If there are no photos at all, treat it as a **book without images**

Branch only once at the book level.  
Do not select different templates per date.

---

## API Design Principles

Follow REST style.

### Authentication

- `POST /api/auth/signup`
- `POST /api/auth/login`
- `GET /api/auth/me`

### Activities

- `GET /api/activities/months`
- `GET /api/activities?month=YYYY-MM`
- `GET /api/activities/{activityId}`
- `GET /api/activities/stats?month=YYYY-MM`

### Albums

- `POST /api/albums`
- `GET /api/albums/{albumId}`
- `PATCH /api/albums/{albumId}`
- `POST /api/albums/{albumId}/activities`
- `POST /api/albums/{albumId}/activities/{activityId}/photos`
- `DELETE /api/albums/{albumId}/activities/{activityId}/photos/{photoId}`

### Book Generation / Orders

- `POST /api/albums/{albumId}/book`
- `POST /api/albums/{albumId}/orders`

---

## Folder / Package Structure Principles

Example:

```text
src/main/java/.../
 ┣ auth
 ┃ ┣ controller
 ┃ ┣ service
 ┃ ┣ domain
 ┃ ┣ dto
 ┃ ┗ security
 ┣ activity
 ┃ ┣ controller
 ┃ ┣ service
 ┃ ┣ domain
 ┃ ┣ repository
 ┃ ┗ dto
 ┣ album
 ┃ ┣ controller
 ┃ ┣ service
 ┃ ┣ domain
 ┃ ┣ repository
 ┃ ┗ dto
 ┣ photo
 ┣ order
 ┣ sweetbook
 ┃ ┣ client
 ┃ ┣ dto
 ┃ ┗ service
 ┗ common
   ┣ config
   ┣ exception
   ┣ response
   ┗ util
```

### Rules

- controller: only handles HTTP input/output
- service: handles business logic
- repository: handles database access
- dto: request/response objects
- client: only for external API calls
- common: global configuration and common exception handling

---

## CSV Import Principles

- Use the uploaded workout CSV as seed data or initial import data.
- Check CSV column names first and map only the required fields.
- Normalize distance, time, speed, and similar values into query-friendly formats.
- You may add helper columns such as `activity_month` for easier querying.
- Do not expose raw CSV structure directly in API responses. Use separate response DTOs.

---

## Sweetbook Integration Principles

### Common

- Inject the API Key from environment variables.
- Use the format `Authorization: Bearer {API_KEY}`
- Wrap external communication failures as domain exceptions.
- Keep request/response logs only at the necessary level and exclude sensitive information.

### Implementation Order

1. Create book
2. Add cover
3. Add month-start page
4. Repeatedly add content pages based on selected workouts
5. Finalize
6. Create order

### Notes

- Separate Sweetbook API DTOs from internal service DTOs.
- Hardcode template parameters only if necessary, and manage them centrally as constants.
- Introduce retry logic for external API calls only at a minimal and reasonable level if needed.

---

## Security Rules

- Encrypt passwords with BCrypt.
- Manage JWT Secret through environment variables.
- Do not commit `.env`, real key files, or deployment secrets.
- Apply authentication filters to APIs that require authorization.
- Always verify ownership when accessing user-specific resources.

---

## Exception Handling Principles

- Keep a common exception response format.
- Handle predictable exceptions with explicit custom exceptions.
- Examples:
  - User not found
  - Password mismatch
  - Album not found
  - No permission
  - No data for the month
  - Sweetbook call failed
  - File upload failed

---

## Implementation Priority

1. Authentication
2. Activity query
3. Album create/query/update
4. AlbumActivity selection
5. Photo upload
6. Sweetbook book generation
7. Order creation
8. Tests / exception handling cleanup

---

## Testing Principles

- Prioritize tests for core service-layer logic
- Template branching tests are required
- Ownership verification tests are required
- Monthly statistics calculation tests are required
- Use mocks or stubs for external APIs whenever possible

---

## Code Style

- Keep methods short and clear
- Do not use meaningless abbreviations
- Do not overuse Optional
- Make null-handling rules explicit
- Use enums for state values whenever appropriate
- Extract hardcoded strings into constants
- Write comments to explain why, not what

---

## Work Process

- First read and summarize the current project structure.
- Then modify only the necessary files.
- Briefly explain the reason and scope of each change in **Korean**.
- After implementation, provide:
  - changed files
  - explanation of core logic
  - points to verify
  - remaining TODOs

---

## Prohibited Actions

- Modifying the frontend unless explicitly requested
- Adding unnecessary libraries
- Making the authentication structure overly complex
- Implementing external API specs by guessing
- Changing the data model drastically without reason
- Refactoring the entire project at once

---

## Final Goal

This backend must reliably support the following user experience:

1. The user logs in with email.
2. The user views their monthly workout data.
3. The user selects a specific month.
4. The user selects workout dates to include in the book.
5. The user attaches photos only to desired dates.
6. The user enters the title and monthly review.
7. The user generates and orders the book.

Always prioritize work based on this core flow.
