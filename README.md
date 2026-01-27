## PT-KSS

Spring Boot application (Java 21, Spring Boot 3.5) that exposes a REST API endpoint for uploading a single file which is then kept in memory for further processing.

### Build and run

- **Build**:

```bash
mvn clean package
```

- **Run** (default port 8080):

```bash
mvn spring-boot:run
```

### Upload API

- **Method**: `POST`
- **Path**: `/api/v1/upload`
- **Consumes**: `multipart/form-data`
- **Parameter**:
  - `file`: single file part
- **Allowed content types**:
  - `application/zip`
  - `application/octet-stream`
- **Max size**:
  - 10 MB, configurable via application configuration (see below).

On successful upload, the file bytes are kept in memory for later internal processing and the endpoint returns JSON containing file metadata (such as internal ID, original filename, content type, size, upload time).

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
  - Hard limit enforced by Spring’s multipart handling.
- **`kss.upload.max-size-bytes`**:
  - Logical maximum upload size used by the application code (and can be changed without modifying code).

### Example `curl` call

Assuming the application is running on `http://localhost:8080` and you have a zip file at `./archive.zip`:

```bash
curl -v \
  -X POST "http://localhost:8080/api/v1/upload" \
  -H "Content-Type: multipart/form-data" \
  -F "file=@./archive.zip;type=application/zip"
```

Using `application/octet-stream` instead:

```bash
curl -v \
  -X POST "http://localhost:8080/api/v1/upload" \
  -H "Content-Type: multipart/form-data" \
  -F "file=@./binary.dat;type=application/octet-stream"
```

If the file exceeds the configured 10 MB limit or has an unsupported content type, the API responds with an appropriate HTTP error status and a JSON error body.

