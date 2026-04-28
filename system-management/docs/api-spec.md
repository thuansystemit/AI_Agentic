# API Specification

**Agent:** `@api-designer`
**Project:** System Management
**Date:** 2026-04-25
**Input:** requirements.md, architecture.md

---

## Conventions

- Base path: `/api/v1`
- Authentication: HTTP-only cookie `access_token` (JWT)
- All request/response bodies: `application/json`
- Timestamps: ISO 8601 UTC (`2026-04-25T10:00:00Z`)
- IDs: UUID v4
- Pagination: offset-based (`?page=0&size=20`)
- Errors: `{ "code": string, "message": string, "fields": [...] }`

---

## Endpoint Summary

| Method | Path | Description | Auth | Role |
|--------|------|-------------|------|------|
| POST | `/auth/login` | Login | No | — |
| POST | `/auth/refresh` | Refresh tokens | Cookie | — |
| POST | `/auth/logout` | Logout | Cookie | — |
| GET | `/auth/me` | Current user | Yes | Any |
| GET | `/users` | List users | Yes | ADMIN |
| POST | `/users` | Create user | Yes | ADMIN |
| GET | `/users/{id}` | Get user | Yes | ADMIN |
| PATCH | `/users/{id}` | Update user | Yes | ADMIN |
| GET | `/groups` | List groups | Yes | ADMIN |
| POST | `/groups` | Create group | Yes | ADMIN |
| GET | `/groups/{id}` | Get group | Yes | ADMIN |
| PATCH | `/groups/{id}` | Update group | Yes | ADMIN |
| DELETE | `/groups/{id}` | Delete group | Yes | ADMIN |
| POST | `/groups/{id}/members` | Add member | Yes | ADMIN |
| DELETE | `/groups/{id}/members/{userId}` | Remove member | Yes | ADMIN |
| GET | `/categories` | List accessible categories | Yes | Any |
| POST | `/categories` | Create category | Yes | ADMIN |
| GET | `/categories/{id}` | Get category | Yes | READ+ |
| PATCH | `/categories/{id}` | Update category | Yes | ADMIN |
| DELETE | `/categories/{id}` | Delete category | Yes | ADMIN |
| GET | `/categories/{id}/permissions/users` | List user permissions | Yes | EDIT |
| PUT | `/categories/{id}/permissions/users/{userId}` | Set user permission | Yes | EDIT |
| DELETE | `/categories/{id}/permissions/users/{userId}` | Remove user permission | Yes | EDIT |
| GET | `/categories/{id}/permissions/groups` | List group permissions | Yes | EDIT |
| PUT | `/categories/{id}/permissions/groups/{groupId}` | Set group permission | Yes | EDIT |
| DELETE | `/categories/{id}/permissions/groups/{groupId}` | Remove group permission | Yes | EDIT |
| GET | `/categories/{id}/documents` | List documents | Yes | READ+ |
| POST | `/categories/{id}/documents` | Create document | Yes | WRITE+ |
| GET | `/documents/{id}` | Get document | Yes | READ+ |
| PATCH | `/documents/{id}` | Update document | Yes | WRITE+ |
| DELETE | `/documents/{id}` | Delete document | Yes | EDIT |

---

## OpenAPI 3.1 Specification

```yaml
openapi: 3.1.0
info:
  title: System Management API
  version: 1.0.0
  description: Document management system with role-based and category-level permissions

servers:
  - url: http://localhost:8080/api/v1
    description: Local development
  - url: https://api.darkness.com/api/v1
    description: Production

components:
  schemas:

    AuthResponse:
      type: object
      required: [id, email, fullName, role]
      properties:
        id:
          type: string
          format: uuid
        email:
          type: string
          format: email
        fullName:
          type: string
        role:
          $ref: '#/components/schemas/GlobalRole'

    UserResponse:
      type: object
      required: [id, email, fullName, role, isActive, createdAt]
      properties:
        id:
          type: string
          format: uuid
        email:
          type: string
        fullName:
          type: string
        role:
          $ref: '#/components/schemas/GlobalRole'
        isActive:
          type: boolean
        createdAt:
          type: string
          format: date-time

    GroupResponse:
      type: object
      required: [id, name, createdAt]
      properties:
        id:
          type: string
          format: uuid
        name:
          type: string
        description:
          type: string
        createdAt:
          type: string
          format: date-time

    CategoryResponse:
      type: object
      required: [id, name, createdAt]
      properties:
        id:
          type: string
          format: uuid
        name:
          type: string
        description:
          type: string
        createdAt:
          type: string
          format: date-time

    DocumentResponse:
      type: object
      required: [id, title, content, categoryId, createdBy, createdAt, updatedAt]
      properties:
        id:
          type: string
          format: uuid
        title:
          type: string
        content:
          type: string
        categoryId:
          type: string
          format: uuid
        createdBy:
          type: string
          format: uuid
        createdAt:
          type: string
          format: date-time
        updatedAt:
          type: string
          format: date-time

    PermissionEntryResponse:
      type: object
      required: [subjectId, subjectType, permission]
      properties:
        subjectId:
          type: string
          format: uuid
        subjectType:
          type: string
          enum: [USER, GROUP]
        permission:
          $ref: '#/components/schemas/Permission'

    PageResponse:
      type: object
      required: [data, pagination]
      properties:
        data:
          type: array
          items: {}
        pagination:
          type: object
          properties:
            page:
              type: integer
            size:
              type: integer
            totalElements:
              type: integer
              format: int64
            totalPages:
              type: integer

    GlobalRole:
      type: string
      enum: [ADMIN, EDITOR, VIEWER]

    Permission:
      type: string
      enum: [READ, WRITE, EDIT]

    Error:
      type: object
      required: [code, message]
      properties:
        code:
          type: string
        message:
          type: string
        fields:
          type: array
          items:
            type: object
            properties:
              field:
                type: string
              message:
                type: string

  responses:
    Unauthorized:
      description: Missing or invalid access token
      content:
        application/json:
          schema:
            $ref: '#/components/schemas/Error'
    Forbidden:
      description: Insufficient permission
      content:
        application/json:
          schema:
            $ref: '#/components/schemas/Error'
    NotFound:
      description: Resource not found
      content:
        application/json:
          schema:
            $ref: '#/components/schemas/Error'
    Conflict:
      description: Resource already exists
      content:
        application/json:
          schema:
            $ref: '#/components/schemas/Error'

paths:

  /auth/login:
    post:
      summary: Login
      tags: [Auth]
      requestBody:
        required: true
        content:
          application/json:
            schema:
              type: object
              required: [email, password]
              properties:
                email:
                  type: string
                  format: email
                password:
                  type: string
                  minLength: 8
      responses:
        '200':
          description: Login successful — tokens set in HTTP-only cookies
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/AuthResponse'
        '401':
          $ref: '#/components/responses/Unauthorized'
        '423':
          description: Account locked
          headers:
            Retry-After:
              schema:
                type: integer
              description: Seconds until account unlocks

  /auth/refresh:
    post:
      summary: Refresh access token
      tags: [Auth]
      responses:
        '204':
          description: Tokens rotated — new cookies set
        '401':
          $ref: '#/components/responses/Unauthorized'

  /auth/logout:
    post:
      summary: Logout
      tags: [Auth]
      responses:
        '204':
          description: Refresh token revoked — cookies cleared

  /auth/me:
    get:
      summary: Get current authenticated user
      tags: [Auth]
      responses:
        '200':
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/AuthResponse'
        '401':
          $ref: '#/components/responses/Unauthorized'

  /users:
    get:
      summary: List users
      tags: [Users]
      parameters:
        - name: search
          in: query
          schema:
            type: string
        - name: page
          in: query
          schema:
            type: integer
            default: 0
        - name: size
          in: query
          schema:
            type: integer
            default: 20
      responses:
        '200':
          content:
            application/json:
              schema:
                allOf:
                  - $ref: '#/components/schemas/PageResponse'
                  - properties:
                      data:
                        items:
                          $ref: '#/components/schemas/UserResponse'
        '403':
          $ref: '#/components/responses/Forbidden'

    post:
      summary: Create user
      tags: [Users]
      requestBody:
        required: true
        content:
          application/json:
            schema:
              type: object
              required: [email, fullName, role]
              properties:
                email:
                  type: string
                  format: email
                fullName:
                  type: string
                  minLength: 2
                role:
                  $ref: '#/components/schemas/GlobalRole'
      responses:
        '201':
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/UserResponse'
        '409':
          $ref: '#/components/responses/Conflict'

  /users/{id}:
    get:
      summary: Get user by ID
      tags: [Users]
      parameters:
        - name: id
          in: path
          required: true
          schema:
            type: string
            format: uuid
      responses:
        '200':
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/UserResponse'
        '404':
          $ref: '#/components/responses/NotFound'

    patch:
      summary: Update user
      tags: [Users]
      parameters:
        - name: id
          in: path
          required: true
          schema:
            type: string
            format: uuid
      requestBody:
        content:
          application/json:
            schema:
              type: object
              properties:
                fullName:
                  type: string
                role:
                  $ref: '#/components/schemas/GlobalRole'
                isActive:
                  type: boolean
      responses:
        '200':
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/UserResponse'
        '400':
          description: Cannot modify own role or deactivate self

  /groups:
    get:
      summary: List groups
      tags: [Groups]
      responses:
        '200':
          content:
            application/json:
              schema:
                allOf:
                  - $ref: '#/components/schemas/PageResponse'
                  - properties:
                      data:
                        items:
                          $ref: '#/components/schemas/GroupResponse'

    post:
      summary: Create group
      tags: [Groups]
      requestBody:
        required: true
        content:
          application/json:
            schema:
              type: object
              required: [name]
              properties:
                name:
                  type: string
                description:
                  type: string
      responses:
        '201':
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/GroupResponse'
        '409':
          $ref: '#/components/responses/Conflict'

  /groups/{id}:
    get:
      summary: Get group
      tags: [Groups]
      parameters:
        - name: id
          in: path
          required: true
          schema:
            type: string
            format: uuid
      responses:
        '200':
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/GroupResponse'
        '404':
          $ref: '#/components/responses/NotFound'

    patch:
      summary: Update group
      tags: [Groups]
      parameters:
        - name: id
          in: path
          required: true
          schema:
            type: string
            format: uuid
      requestBody:
        content:
          application/json:
            schema:
              type: object
              properties:
                name:
                  type: string
                description:
                  type: string
      responses:
        '200':
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/GroupResponse'

    delete:
      summary: Delete group
      tags: [Groups]
      parameters:
        - name: id
          in: path
          required: true
          schema:
            type: string
            format: uuid
      responses:
        '204':
          description: Group deleted — all permissions revoked

  /groups/{id}/members:
    post:
      summary: Add user to group
      tags: [Groups]
      parameters:
        - name: id
          in: path
          required: true
          schema:
            type: string
            format: uuid
      requestBody:
        required: true
        content:
          application/json:
            schema:
              type: object
              required: [userId]
              properties:
                userId:
                  type: string
                  format: uuid
      responses:
        '204':
          description: Member added (idempotent)

  /groups/{id}/members/{userId}:
    delete:
      summary: Remove user from group
      tags: [Groups]
      parameters:
        - name: id
          in: path
          required: true
          schema:
            type: string
            format: uuid
        - name: userId
          in: path
          required: true
          schema:
            type: string
            format: uuid
      responses:
        '204':
          description: Member removed

  /categories:
    get:
      summary: List accessible categories
      tags: [Categories]
      parameters:
        - name: page
          in: query
          schema:
            type: integer
            default: 0
        - name: size
          in: query
          schema:
            type: integer
            default: 20
      responses:
        '200':
          description: ADMIN sees all; others see only categories with READ+
          content:
            application/json:
              schema:
                allOf:
                  - $ref: '#/components/schemas/PageResponse'
                  - properties:
                      data:
                        items:
                          $ref: '#/components/schemas/CategoryResponse'

    post:
      summary: Create category (ADMIN only)
      tags: [Categories]
      requestBody:
        required: true
        content:
          application/json:
            schema:
              type: object
              required: [name]
              properties:
                name:
                  type: string
                description:
                  type: string
      responses:
        '201':
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/CategoryResponse'
        '409':
          $ref: '#/components/responses/Conflict'

  /categories/{id}:
    get:
      summary: Get category
      tags: [Categories]
      parameters:
        - name: id
          in: path
          required: true
          schema:
            type: string
            format: uuid
      responses:
        '200':
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/CategoryResponse'
        '403':
          $ref: '#/components/responses/Forbidden'
        '404':
          $ref: '#/components/responses/NotFound'

    patch:
      summary: Update category (ADMIN only)
      tags: [Categories]
      parameters:
        - name: id
          in: path
          required: true
          schema:
            type: string
            format: uuid
      requestBody:
        content:
          application/json:
            schema:
              type: object
              properties:
                name:
                  type: string
                description:
                  type: string
      responses:
        '200':
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/CategoryResponse'

    delete:
      summary: Delete category (ADMIN only)
      tags: [Categories]
      parameters:
        - name: id
          in: path
          required: true
          schema:
            type: string
            format: uuid
      responses:
        '204':
          description: Category and all documents/permissions deleted

  /categories/{id}/permissions/users:
    get:
      summary: List user permissions on category
      tags: [Permissions]
      parameters:
        - name: id
          in: path
          required: true
          schema:
            type: string
            format: uuid
      responses:
        '200':
          content:
            application/json:
              schema:
                type: array
                items:
                  $ref: '#/components/schemas/PermissionEntryResponse'
        '403':
          $ref: '#/components/responses/Forbidden'

  /categories/{id}/permissions/users/{userId}:
    put:
      summary: Set user permission on category (EDIT required)
      tags: [Permissions]
      parameters:
        - name: id
          in: path
          required: true
          schema:
            type: string
            format: uuid
        - name: userId
          in: path
          required: true
          schema:
            type: string
            format: uuid
      requestBody:
        required: true
        content:
          application/json:
            schema:
              type: object
              required: [permission]
              properties:
                permission:
                  $ref: '#/components/schemas/Permission'
      responses:
        '200':
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/PermissionEntryResponse'

    delete:
      summary: Remove user permission on category (EDIT required)
      tags: [Permissions]
      parameters:
        - name: id
          in: path
          required: true
          schema:
            type: string
            format: uuid
        - name: userId
          in: path
          required: true
          schema:
            type: string
            format: uuid
      responses:
        '204':
          description: Permission removed

  /categories/{id}/permissions/groups:
    get:
      summary: List group permissions on category
      tags: [Permissions]
      parameters:
        - name: id
          in: path
          required: true
          schema:
            type: string
            format: uuid
      responses:
        '200':
          content:
            application/json:
              schema:
                type: array
                items:
                  $ref: '#/components/schemas/PermissionEntryResponse'

  /categories/{id}/permissions/groups/{groupId}:
    put:
      summary: Set group permission on category (EDIT required)
      tags: [Permissions]
      parameters:
        - name: id
          in: path
          required: true
          schema:
            type: string
            format: uuid
        - name: groupId
          in: path
          required: true
          schema:
            type: string
            format: uuid
      requestBody:
        required: true
        content:
          application/json:
            schema:
              type: object
              required: [permission]
              properties:
                permission:
                  $ref: '#/components/schemas/Permission'
      responses:
        '200':
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/PermissionEntryResponse'

    delete:
      summary: Remove group permission on category (EDIT required)
      tags: [Permissions]
      parameters:
        - name: id
          in: path
          required: true
          schema:
            type: string
            format: uuid
        - name: groupId
          in: path
          required: true
          schema:
            type: string
            format: uuid
      responses:
        '204':
          description: Permission removed

  /categories/{id}/documents:
    get:
      summary: List documents in category (READ required)
      tags: [Documents]
      parameters:
        - name: id
          in: path
          required: true
          schema:
            type: string
            format: uuid
        - name: page
          in: query
          schema:
            type: integer
            default: 0
        - name: size
          in: query
          schema:
            type: integer
            default: 20
      responses:
        '200':
          content:
            application/json:
              schema:
                allOf:
                  - $ref: '#/components/schemas/PageResponse'
                  - properties:
                      data:
                        items:
                          $ref: '#/components/schemas/DocumentResponse'
        '403':
          $ref: '#/components/responses/Forbidden'

    post:
      summary: Create document in category (WRITE required)
      tags: [Documents]
      parameters:
        - name: id
          in: path
          required: true
          schema:
            type: string
            format: uuid
      requestBody:
        required: true
        content:
          application/json:
            schema:
              type: object
              required: [title]
              properties:
                title:
                  type: string
                  minLength: 1
                content:
                  type: string
      responses:
        '201':
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/DocumentResponse'
        '403':
          $ref: '#/components/responses/Forbidden'

  /documents/{id}:
    get:
      summary: Get document by ID (READ on its category required)
      tags: [Documents]
      parameters:
        - name: id
          in: path
          required: true
          schema:
            type: string
            format: uuid
      responses:
        '200':
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/DocumentResponse'
        '403':
          $ref: '#/components/responses/Forbidden'
        '404':
          $ref: '#/components/responses/NotFound'

    patch:
      summary: Update document (WRITE on its category required)
      tags: [Documents]
      parameters:
        - name: id
          in: path
          required: true
          schema:
            type: string
            format: uuid
      requestBody:
        content:
          application/json:
            schema:
              type: object
              properties:
                title:
                  type: string
                content:
                  type: string
      responses:
        '200':
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/DocumentResponse'
        '403':
          $ref: '#/components/responses/Forbidden'

    delete:
      summary: Delete document (EDIT on its category required)
      tags: [Documents]
      parameters:
        - name: id
          in: path
          required: true
          schema:
            type: string
            format: uuid
      responses:
        '204':
          description: Document deleted
        '403':
          $ref: '#/components/responses/Forbidden'
```
