# Tasklane

Tasklane is a full-stack task management MVP. It combines a responsive Angular workspace, a secure Spring Boot API, MongoDB persistence, role-based access control, Docker Compose, Kubernetes resources, and a GitHub Actions pipeline.

## MVP features

- Email/password registration and sign-in with BCrypt password hashing and JWT sessions
- Personal task ownership for members
- Workspace-wide task visibility for administrators
- Action-oriented dashboard with daily briefing, upcoming work, project progress, story-point totals, and recent activity
- Interactive Kanban board with drag-to-move cards and touch-friendly status controls
- List view, due-date shortcuts, project and priority filters, search, and flexible sorting
- Administrator assignment to active team members with server-enforced ownership
- Task labels, Jira-style story points, due dates, priorities, creation, editing, and deletion
- Administrator user directory with role and account-status controls
- Responsive, white-themed interface for desktop and mobile
- Health checks, seeded demo data, containers, Kubernetes, and CI

## Technology

- Angular 20 and TypeScript
- Spring Boot 3.2, Spring Security, and Java 21
- MongoDB 7
- Docker Compose and Kubernetes
- GitHub Actions

## Run the complete application

Prerequisite: Docker Desktop with Docker Compose.

```bash
docker compose up --build
```

Open [http://localhost:4200](http://localhost:4200). The public health endpoint is [http://localhost:8080/actuator/health](http://localhost:8080/actuator/health).

Docker starts MongoDB for you. MongoDB Compass is only a database viewer and does **not** need to be open. Data is stored in the `tasklane-data` Docker volume.

Stop the application with:

```bash
docker compose down
```

## Demo accounts

These accounts are created on startup when demo data is enabled:

| Name | Access | Email | Password |
| --- | --- | --- | --- |
| Tasklane Admin | Administrator | `admin@tasklane.local` | `Admin@123` |
| Maya Chen | Member | `demo@tasklane.local` | `Demo@123` |
| Noah Williams | Member | `noah@tasklane.local` | `Demo@123` |
| Ava Patel | Member | `ava@tasklane.local` | `Demo@123` |
| Ethan Brown | Member | `ethan@tasklane.local` | `Demo@123` |

The credentials and JWT secret are intended for local demos. Override `JWT_SECRET`, `DEMO_ADMIN_PASSWORD`, and `DEMO_USER_PASSWORD` before deploying anywhere public.

## Run for development

When running without Docker, the MongoDB **server** must be running on port `27017`. Compass may be closed. On macOS with Homebrew, check or start it with:

```bash
brew services list | grep mongodb
brew services start mongodb-community
```

Run the API:

```bash
cd backend
./mvnw spring-boot:run
```

In another terminal, run the web application:

```bash
cd frontend
npm install
npm start
```

Open [http://localhost:4200](http://localhost:4200). The Angular development server proxies `/api` requests to port `8080`.

## API

Public endpoints:

| Method | Endpoint | Purpose |
| --- | --- | --- |
| `POST` | `/api/auth/register` | Create a member account |
| `POST` | `/api/auth/login` | Sign in and receive a JWT |
| `GET` | `/actuator/health` | Check API health |

Authenticated endpoints:

| Method | Endpoint | Purpose |
| --- | --- | --- |
| `GET` | `/api/auth/me` | Get the signed-in account |
| `GET` | `/api/tasks` | List visible tasks |
| `POST` | `/api/tasks` | Create a task |
| `PUT` | `/api/tasks/{id}` | Update an owned or visible task |
| `DELETE` | `/api/tasks/{id}` | Delete an owned or visible task |
| `GET` | `/api/admin/users` | List accounts (admin only) |
| `PATCH` | `/api/admin/users/{id}` | Change role or status (admin only) |

Example login:

```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"email":"demo@tasklane.local","password":"Demo@123"}'
```

Use the returned token as `Authorization: Bearer <token>` for protected API requests.

## Kubernetes

Start a Docker-backed Minikube cluster and build both application images directly into its container runtime:

```bash
minikube start --driver=docker
minikube image build -t arunodhai/tasklane-api:latest backend
minikube image build -t arunodhai/tasklane-web:latest frontend
```

Apply the resources and wait for MongoDB, the API, and the web application to become ready:

```bash
kubectl apply -f deployment/kubernetes/tasklane.yml
kubectl rollout status deployment/tasklane-mongodb --timeout=300s
kubectl rollout status deployment/tasklane-api --timeout=300s
kubectl rollout status deployment/tasklane-web --timeout=300s
kubectl get deployments,pods,services,pvc
```

Open a local tunnel to the NodePort service:

```bash
minikube service tasklane-web --url
```

With the Docker driver on macOS, keep that terminal open while using the printed URL. Stop the cluster without deleting its data with:

```bash
minikube stop
```

The credentials in `deployment/kubernetes/tasklane.yml` are for local demonstrations only. Replace them before using the manifest in a shared environment.

## Verification

```bash
cd backend && ./mvnw clean test
cd ../frontend && npm test -- --watch=false --browsers=ChromeHeadless
npm run build
```

## Interview demo flow

1. Sign in as Ava Patel and show that the workspace contains only work assigned to Ava.
2. Demonstrate dashboard metrics, story points, filtering, list and board views, and the complete task lifecycle.
3. Sign out and sign in as the administrator, then assign work to a named team member.
4. Open Access to explain role-based authorization, promote or disable a member, and show workspace-wide task visibility.
5. Show the health endpoint, Docker Compose file, Kubernetes manifest, and GitHub Actions workflow to explain delivery and operations.
