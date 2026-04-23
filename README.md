# WebsiteBooksManager

A Java-based web application for managing books, including uploading, storing, and organizing book data with cloud storage integration.

---

## 🚀 Features
- Book management system
- File upload support
- Cloudflare R2 integration
- Modular backend structure
- HTTPS-enabled local deployment support

---

## 🛠️ Tech Stack
- Java
- Spring Boot / Servlet-based backend
- Maven
- Cloudflare R2 (object storage)
- TLS/SSL (Cloudflare Origin Certificate)

---

## 🔐 Security & Configuration

Sensitive credentials are not stored in this repository.

The application uses environment variables for configuration:

```properties
cloudflare.accountId=${CLOUDFLARE_ACCOUNTID}
cloudflare.accessKey=${CLOUDFLARE_ACCESSKEY}
cloudflare.secretKey=${CLOUDFLARE_SECRETKEY}