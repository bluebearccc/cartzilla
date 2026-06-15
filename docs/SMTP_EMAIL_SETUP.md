# SMTP Email Setup

Notification emails are sent by `notification-service` through Spring Mail.

Local development defaults to Mailhog:

```env
MAIL_HOST=localhost
MAIL_PORT=1025
MAIL_USERNAME=
MAIL_PASSWORD=
MAIL_FROM=no-reply@cartzilla.local
MAIL_SMTP_AUTH=false
MAIL_SMTP_STARTTLS_ENABLE=false
```

Mailhog UI:

```text
http://localhost:8025
```

## Real SMTP Provider

Set these values in your local `.env` or deployment secret store. Do not commit real credentials.

```env
MAIL_HOST=smtp.gmail.com
MAIL_PORT=587
MAIL_USERNAME=your-email@gmail.com
MAIL_PASSWORD=your-google-app-password
MAIL_FROM=your-email@gmail.com
MAIL_SMTP_AUTH=true
MAIL_SMTP_STARTTLS_ENABLE=true
```

For Gmail, use a Google App Password instead of your normal Google password.
When running with Docker Compose, `notification-service` reads these same variables from `.env`; without overrides it falls back to the Compose Mailhog service.

Other providers such as SendGrid, Mailgun, or AWS SES can use the same variables with their SMTP host, port, username, and password.
