# WebsiteBooksManager

A Java-based web application for managing books, including features for uploading, storing, and organizing book data.

## Features
- Book management system
- File upload support
- Cloud storage integration (Cloudflare R2)
- Modular project structure

## Tech Stack
- Java
- Maven
- Cloudflare R2 (object storage)
- Spring / Servlet-based backend (if applicable)

## Security Note
Sensitive credentials are not stored in the repository.  
Environment variables or external configuration files are used instead.

## Setup
1. Clone the repo
2. Configure environment variables for cloud services
3. Build with Maven:
   ```bash
   mvn clean install