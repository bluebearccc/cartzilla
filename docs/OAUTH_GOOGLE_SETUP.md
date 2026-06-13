# Google OAuth Local Setup

This guide configures real Google OAuth for local Cartzilla development without committing secrets.

## 1. Shared Google OAuth Client

Use one shared dev OAuth client for the team:

- Application type: Web application
- Name: Cartzilla Local Shared Dev
- Authorized JavaScript origins:
  - `http://localhost:5173`
  - `http://localhost:8080`
- Authorized redirect URI:
  - `http://localhost:8080/api/oauth/google/callback`

The redirect URI must match exactly. The backend default also uses:

```text
http://localhost:8080/api/oauth/google/callback
```

## 2. Local Environment

Copy the example file:

```powershell
Copy-Item .env.example .env
```

Fill these values in `.env`:

```env
GOOGLE_OAUTH_CLIENT_ID=your-client-id.apps.googleusercontent.com
GOOGLE_OAUTH_CLIENT_SECRET=your-client-secret
GOOGLE_OAUTH_REDIRECT_URI=http://localhost:8080/api/oauth/google/callback
```

Do not commit `.env`. The repository ignores local env files.

## 3. Running From IntelliJ

If running `user-service` from IntelliJ instead of Docker Compose, add these environment variables to the `user-service` Run Configuration:

```text
GOOGLE_OAUTH_CLIENT_ID=your-client-id.apps.googleusercontent.com;GOOGLE_OAUTH_CLIENT_SECRET=your-client-secret;GOOGLE_OAUTH_REDIRECT_URI=http://localhost:8080/api/oauth/google/callback
```

The callback goes through API Gateway on port `8080`, even though `user-service` runs on port `8081`.

## 4. Test The Flow

Start the required services:

- `api-gateway` on `8080`
- `user-service` on `8081`
- `postgres-user`
- service discovery/config dependencies used by your local run mode

Open:

```text
http://localhost:8080/api/oauth/google/authorize
```

The API returns an `authorizationUrl`. Open that URL in a browser, sign in with the Google account allowed by the OAuth consent screen, and Google will call back to:

```text
http://localhost:8080/api/oauth/google/callback?code=...
```

Expected callback response:

```json
{
  "success": true,
  "data": {
    "accessToken": "...",
    "refreshToken": "...",
    "email": "your_email@gmail.com",
    "role": "CUSTOMER"
  }
}
```

## 5. Common Errors

- `redirect_uri_mismatch`: Google Cloud redirect URI does not exactly match `GOOGLE_OAUTH_REDIRECT_URI`.
- `OAuth provider setting is missing: client-id`: `user-service` did not receive `GOOGLE_OAUTH_CLIENT_ID`.
- `OAuth provider setting is missing: client-secret`: `user-service` did not receive `GOOGLE_OAUTH_CLIENT_SECRET`.
- `Access blocked`: the Google account is not in Test users while the OAuth app is in Testing mode.

## 6. Production

For production, add a production redirect URI in Google Cloud, for example:

```text
https://api.cartzilla.com/api/oauth/google/callback
```

Then set:

```env
GOOGLE_OAUTH_REDIRECT_URI=https://api.cartzilla.com/api/oauth/google/callback
```
