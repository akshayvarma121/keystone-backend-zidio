# Keystone Deployment Guide

This guide covers how to run the backend locally for testing, and how to deploy it live to the web.

## 1. Local Testing (Waking up the backend)

To test the application locally, you need two things running: the PostgreSQL database and the Spring Boot application.

### Step 1: Start the Database
The project uses `docker-compose` to spin up a PostgreSQL database instantly.
Make sure Docker Desktop is open and running, then execute:
```bash
docker-compose up -d
```
*(This starts the database in the background on port 5432).*

### Step 2: Start the Backend
You can run the application directly using Maven:
```bash
mvn clean install -DskipTests
mvn spring-boot:run
```
*(Flyway will automatically run all database migrations from V1 to V13 when it boots, and it will seed the test logins).*

**Access the API:** 
- Swagger Documentation: http://localhost:8080/swagger-ui.html
- Health Check: http://localhost:8080/actuator/health

---

## 2. Deploying Live to the Web

To deploy Keystone to a production server (like AWS EC2, DigitalOcean, or Render), you should use the provided Dockerfile.

### Prerequisites on Production Server
1. A Linux server (Ubuntu recommended)
2. Docker installed
3. A managed PostgreSQL database (e.g., AWS RDS, DigitalOcean Managed Database) OR a PostgreSQL Docker container on the server.
4. AWS S3 bucket for storing attachments (optional but recommended for production).

### Step 1: Build the Docker Image
The `Dockerfile` in this repo uses a multi-stage build. It compiles the app and packages it into a tiny, secure Java Runtime environment.
```bash
docker build -t keystone-backend:latest .
```

### Step 2: Run the Docker Container
When running in production, you must override the environment variables so the app connects to your live database instead of `localhost`.

```bash
docker run -d \
  --name keystone-api \
  -p 80:8080 \
  -e SPRING_PROFILES_ACTIVE=prod \
  -e DB_URL=jdbc:postgresql://<production-db-host>:5432/keystone \
  -e DB_USER=<production_db_user> \
  -e DB_PASSWORD=<production_db_password> \
  -e JWT_SECRET=<a_very_long_secure_random_base64_string> \
  -e STORAGE_PROVIDER=s3 \
  -e STORAGE_BUCKET=<your-s3-bucket> \
  -e STORAGE_ACCESS_KEY=<aws-access-key> \
  -e STORAGE_SECRET_KEY=<aws-secret-key> \
  --restart unless-stopped \
  keystone-backend:latest
```

### Alternative: Platform-as-a-Service (Render.com)
If you don't want to manage a raw Linux server, **Render.com** is an excellent and free/cheap option. It will pull your code directly from GitHub, build the Docker image, and host it.

#### Step 1: Create a PostgreSQL Database on Render
1. Go to your Render Dashboard and click **New +** > **PostgreSQL**.
2. Name it (e.g., `keystone-db`), select a region, and choose the Free tier.
3. Once created, look for the **Internal Database URL** (it starts with `postgres://...`). You will need this for the backend.

#### Step 2: Deploy the Backend Application
1. Go back to the Dashboard and click **New +** > **Web Service**.
2. Connect your GitHub account and select your `keystone-backend-zidio` repository.
3. Render should automatically detect the `Dockerfile` and set the Environment to "Docker".
4. Scroll down to **Environment Variables** and add the following:

| Key | Value |
| --- | ----- |
| `SPRING_PROFILES_ACTIVE` | `prod` |
| `DB_URL` | *Paste the Internal Database URL from Step 1 but replace `postgres://` with `jdbc:postgresql://`* |
| `DB_USER` | *The username from your Render DB dashboard* |
| `DB_PASSWORD` | *The password from your Render DB dashboard* |
| `JWT_SECRET` | *Generate a random secure string (e.g., `mYvErYsEcReTkEy1234567890qwertyuiop`)* |

*(If you are using S3 for attachments, also add `STORAGE_PROVIDER=s3`, `STORAGE_BUCKET`, `STORAGE_ACCESS_KEY`, and `STORAGE_SECRET_KEY`)*

5. Click **Create Web Service**. 
Render will now build your Docker image and deploy it. Flyway will automatically run the migrations and seed the database upon startup. 
Your API will be live at the `.onrender.com` URL provided at the top of the page!
