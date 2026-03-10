## Drag & Drop File Upload (Spring Boot + MVC)

Modern drag & drop file upload UI built with **Spring Boot**, **Spring MVC**, and **Thymeleaf**.

### Prerequisites

- **Java** 17 or higher installed (`java -version`)
- **Maven** installed (`mvn -version`)

### How to Run

1. **Clone or open the project directory**

```bash
cd ~/Desktop/files
```

2. **Configure the upload directory (optional but recommended)**

By default, the app stores uploaded files under the current folder:

```properties
app.upload.dir=./uploads
```

You can change this in `application.properties` if you want a different location.

3. **Start the application**

```bash
mvn spring-boot:run
```

If everything is correct you should see Spring Boot start Tomcat on port **8080**.

4. **Open the UI in your browser**

Visit:

```text
http://localhost:8080/
```

You’ll see the drag & drop upload page. You can:

- Drag files into the drop zone or click **browse files**
- Upload one or multiple files
- See uploaded files listed with size and date
- Download or delete uploaded files

### Important Endpoints

- `GET /` — Main upload page (Thymeleaf)
- `POST /api/upload` — Upload a single file
- `POST /api/upload/multiple` — Upload multiple files
- `GET /api/files` — List uploaded files (JSON)
- `GET /api/files/{id}/download` — Download a file
- `DELETE /api/files/{id}` — Delete a file

### Build a Jar (optional)

To build a runnable jar:

```bash
mvn clean package
```

Then run:

```bash
java -jar target/file-upload-spring-1.0.0.jar
```

