## PT-KSS

Spring Boot application (Java 21, Spring Boot 3.5) that exposes a REST API for uploading files, checking processing status, and downloading generated EPUBs.

### Build and run

- **Build**:

```bash
mvn clean package
```

- **Run** (default port 8080):

```bash
mvn spring-boot:run
```

### API overview

Base URL: `http://localhost:8080` (configurable via `server.port`)

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/v1/upload` | Upload a zip file for processing |
| GET | `/api/v1/status/{fileId}` | Check processing status |
| GET | `/api/v1/epub/{id}` | Download generated EPUB file |

---

### POST `/api/v1/upload`

Upload a single zip file for processing. The file is kept in memory and processed asynchronously.

| Attribute | Value |
|-----------|-------|
| **Method** | `POST` |
| **Path** | `/api/v1/upload` |
| **Consumes** | `multipart/form-data` |
| **Produces** | `application/json` |
| **Parameter** | `file` – single file part |
| **Allowed content types** | `application/zip`, `application/octet-stream` |
| **Max size** | 10 MB (configurable, see Configuration) |

**Success response** (201 Created):

```json
{
  "id": "abc123",
  "filename": "archive.zip",
  "contentType": "application/zip",
  "size": 1024,
  "uploadTime": "2025-02-03T12:00:00Z"
}
```

**Example**:

```bash
curl -v -X POST "http://localhost:8080/api/v1/upload" \
  -F "file=@./archive.zip;type=application/zip"
```

```bash
curl -v -X POST "http://localhost:8080/api/v1/upload" \
  -F "file=@./binary.dat;type=application/octet-stream"
```

---

### GET `/api/v1/status/{fileId}`

Check the processing status of an uploaded file. Use the `id` returned from the upload response as `fileId`.

| Attribute | Value |
|-----------|-------|
| **Method** | `GET` |
| **Path** | `/api/v1/status/{fileId}` |
| **Produces** | `application/json` |

**Status values**: `in-progress`, `ready`, `error`

**Success response** (200 OK) – in progress:

```json
{
  "status": "in-progress",
  "payload": null,
  "errorMessage": null
}
```

**Success response** (200 OK) – ready:

```json
{
  "status": "ready",
  "payload": {
    "fileId": "abc123",
    "originalFilename": "archive.zip",
    "epubFile": "<base64-encoded-bytes>",
    "storiesList": [],
    "chapters": []
  },
  "errorMessage": null
}
```

**Error response** (500 Internal Server Error):

```json
{
  "status": "error",
  "payload": null,
  "errorMessage": "Processing failed: ..."
}
```

**Example**:

```bash
curl -v "http://localhost:8080/api/v1/status/abc123"
```

---

### GET `/api/v1/epub/{id}`

Download the generated EPUB file. The EPUB is available only when status is `ready`.

| Attribute | Value |
|-----------|-------|
| **Method** | `GET` |
| **Path** | `/api/v1/epub/{id}` |
| **Produces** | `application/epub+zip` |

**Success response** (200 OK): Binary EPUB file with `Content-Disposition: attachment; filename="..."`.

**Example**:

```bash
curl -v -o output.epub "http://localhost:8080/api/v1/epub/abc123"
```

---

### Error responses

On validation or application errors, the API returns JSON in this format:

```json
{
  "timestamp": "2025-02-03T12:00:00Z",
  "status": 413,
  "error": "Payload Too Large",
  "message": "File exceeds maximum allowed size",
  "path": "/api/v1/upload"
}
```

| HTTP status | Condition |
|-------------|-----------|
| 400 Bad Request | Invalid request (e.g. missing file parameter) |
| 404 Not Found | EPUB not found for given ID |
| 202 Accepted | EPUB not yet ready (processing in progress) |
| 413 Payload Too Large | File exceeds configured max size |
| 415 Unsupported Media Type | Content type not `application/zip` or `application/octet-stream` |
| 500 Internal Server Error | Processing error |

---

### Configuration

Upload-specific configuration is exposed via application properties:

```yaml
spring:
  servlet:
    multipart:
      max-file-size: 10MB
      max-request-size: 10MB

kss:
  upload:
    max-size-bytes: 10485760   # 10 MB
```

- **`spring.servlet.multipart.max-file-size` / `max-request-size`**:
  - Hard limit enforced by Spring's multipart handling.
- **`kss.upload.max-size-bytes`**:
  - Logical maximum upload size used by the application code (and can be changed without modifying code).
