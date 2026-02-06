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

**Processing flow**: Upload → in-progress → (if metadata or images missing) awaiting-metadata → user fills via PATCH and/or POST images → approve → in-progress → ready.

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/v1/upload` | Upload a zip file for processing |
| GET | `/api/v1/status/{fileId}` | Check processing status |
| GET | `/api/v1/pending-metadata` | List files awaiting mandatory metadata or images |
| GET | `/api/v1/pending-metadata/{fileId}` | Get metadata, missing fields, and missing images for a file |
| PATCH | `/api/v1/pending-metadata/{fileId}` | Update metadata for a file |
| POST | `/api/v1/pending-metadata/{fileId}/images` | Upload image content for a file |
| POST | `/api/v1/pending-metadata/{fileId}/approve` | Approve and re-queue for processing |
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

**Status values**: `in-progress`, `awaiting-metadata`, `ready`, `error`

When status is `awaiting-metadata`, the user must fill mandatory EPUB metadata (title, creator, publisher, language, identifier) and upload any missing image content via the pending-metadata endpoints before processing can continue.

**Success response** (200 OK) – in progress:

```json
{
  "status": "in-progress",
  "payload": null,
  "errorMessage": null
}
```

**Success response** (200 OK) – awaiting metadata:

```json
{
  "status": "awaiting-metadata",
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
    "chapters": [
      { "title": null, "text": "Paragraph text.", "imageRef": null, "children": null },
      { "title": null, "text": null, "imageRef": "photo.jpg", "children": null }
    ],
    "imageList": [],
    "xhtml": "<base64-encoded-bytes>"
  },
  "errorMessage": null
}
```

The `chapters` array contains `ChapterNode` objects in document order. Each node has:
- `title`: Optional section title (for TOC); null for leaf nodes
- `text`: Paragraph text; null for container or image nodes
- `imageRef`: Image filename (matches imageContent keys); null for text or container nodes
- `children`: Nested sub-chapters/paragraphs; null or empty for leaf nodes

Nodes can be text paragraphs, image references, or containers (chapters/sub-chapters) with nested children.

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

### GET `/api/v1/pending-metadata`

List all files that are awaiting mandatory metadata or image content. These files have reached the metadata check phase but are missing required EPUB fields (title, creator, publisher, language, identifier) and/or image content referenced in the document.

| Attribute | Value |
|-----------|-------|
| **Method** | `GET` |
| **Path** | `/api/v1/pending-metadata` |
| **Produces** | `application/json` |

**Success response** (200 OK):

```json
[
  {
    "fileId": "abc123",
    "originalFilename": "archive.zip"
  }
]
```

**Example**:

```bash
curl -v "http://localhost:8080/api/v1/pending-metadata"
```

---

### GET `/api/v1/pending-metadata/{fileId}`

Get current metadata, list of missing mandatory fields, and list of missing image URIs for a file awaiting user input.

| Attribute | Value |
|-----------|-------|
| **Method** | `GET` |
| **Path** | `/api/v1/pending-metadata/{fileId}` |
| **Produces** | `application/json` |

**Success response** (200 OK):

```json
{
  "fileId": "abc123",
  "originalFilename": "archive.zip",
  "metadata": {
    "title": null,
    "creator": null,
    "publisher": null,
    "language": null,
    "identifier": null
  },
  "missingFields": ["title", "creator", "publisher", "language", "identifier"],
  "missingImages": ["Resources/Graphic/image1.jpg"]
}
```

**Error response** (404 Not Found): Pending metadata not found for the given fileId.

**Example**:

```bash
curl -v "http://localhost:8080/api/v1/pending-metadata/abc123"
```

---

### PATCH `/api/v1/pending-metadata/{fileId}`

Update metadata for a file. All fields are optional; only provided fields are updated.

| Attribute | Value |
|-----------|-------|
| **Method** | `PATCH` |
| **Path** | `/api/v1/pending-metadata/{fileId}` |
| **Consumes** | `application/json` |
| **Produces** | `application/json` |
| **Request body** | Optional fields: `title`, `creator`, `publisher`, `language`, `identifier` |

**Success response** (200 OK): Same shape as GET (updated metadata, missingFields, and missingImages).

**Error response** (404 Not Found): Pending metadata not found for the given fileId.

**Example**:

```bash
curl -v -X PATCH "http://localhost:8080/api/v1/pending-metadata/abc123" \
  -H "Content-Type: application/json" \
  -d '{"title":"My Book","creator":"Author Name","publisher":"Publisher","language":"fi","identifier":"urn:uuid:123"}'
```

---

### POST `/api/v1/pending-metadata/{fileId}/images`

Upload image content for a file awaiting completion. The upload filename must match the last path component of a missing image URI (e.g. URI `Resources/Graphic/image1.jpg` matches filename `image1.jpg`).

| Attribute | Value |
|-----------|-------|
| **Method** | `POST` |
| **Path** | `/api/v1/pending-metadata/{fileId}/images` |
| **Consumes** | `multipart/form-data` |
| **Produces** | `application/json` |
| **Parameter** | `file` – image file part |

**Success response** (200 OK): Same shape as GET (updated metadata, missingFields, and missingImages).

**Error response** (400 Bad Request): Upload filename does not match any missing image URI.

**Error response** (404 Not Found): Pending metadata not found for the given fileId.

**Example**:

```bash
curl -v -X POST "http://localhost:8080/api/v1/pending-metadata/abc123/images" \
  -F "file=@./image1.jpg"
```

---

### POST `/api/v1/pending-metadata/{fileId}/approve`

Remove the file from the pending store and re-queue it for mandatory metadata and image check. Both metadata and all image content must be complete before approval succeeds.

| Attribute | Value |
|-----------|-------|
| **Method** | `POST` |
| **Path** | `/api/v1/pending-metadata/{fileId}/approve` |
| **Produces** | `application/json` |

**Success response** (202 Accepted): Empty body; context is re-queued for mandatory check.

**Error response** (400 Bad Request): Metadata or images still missing; cannot approve.

**Error response** (404 Not Found): Pending metadata not found for the given fileId.

**Example**:

```bash
curl -v -X POST "http://localhost:8080/api/v1/pending-metadata/abc123/approve"
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
| 400 Bad Request | Invalid request (e.g. missing file parameter); image filename does not match any missing image; or approve called when metadata/images still missing |
| 404 Not Found | EPUB not found for given ID; or pending metadata not found (when using pending-metadata endpoints) |
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
