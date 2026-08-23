# Cockpit Dashboard

## First login

A default admin account is created automatically on first startup (STANDALONE mode).

| Field    | Default value          |
|----------|------------------------|
| Username | `admin`                |
| Password | `Admin1234!`           |
| Email    | `admin@cockpit.local`  |
| Role     | SUPER_ADMIN (global)   |

Change the password after first login.

### Override credentials (staging / prod)

Set these environment variables before starting the backend container:

```
COCKPIT_ADMIN_USERNAME=your-username
COCKPIT_ADMIN_EMAIL=your@email.com
COCKPIT_ADMIN_PASSWORD=YourStrongPassword!
```

The seeder is idempotent — it skips creation if the username already exists.

## Auth mode

| Mode         | Description                                              |
|--------------|----------------------------------------------------------|
| `STANDALONE` | Cockpit owns login. Default. Uses the seeded admin above.|
| `INTEGRATED` | Mother app owns login. Admin seeder is skipped entirely. |

Set `COCKPIT_AUTH_MODE=INTEGRATED` to switch modes.
