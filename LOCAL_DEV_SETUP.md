# Running account-service locally

Complete list of every environment variable the app reads, why it's needed,
and where the real value comes from. Written so this isn't tribal knowledge
stuck in one chat session — if you're reading this cold, this is everything.

The app deliberately **fails fast with a clear error** if something required
is missing, rather than starting in a broken state — see each variable below
for what breaks without it.

## Where these come from in production

`.github/workflows/deploy.yml` is the source of truth for this — it shows
exactly which Secret Manager secret backs each sensitive variable
(`--set-secrets`), and lists the genuinely non-sensitive ones directly in
plain text (`--set-env-vars`), all in GCP project `ecstatic-seeker-474703-d4`.
Whenever this doc and that workflow file disagree, trust the workflow file —
it's the one actually driving production, this doc can drift.

**Do not paste real secret values into this file or `.env.example` — both are
committed to the repo.** Put them in your own `.env` (gitignored) or directly
into your IDE's run configuration instead.

## Required to start the app at all

| Variable | What breaks without it | Secret Manager name | Fetch with |
|---|---|---|---|
| `JWT_SECRET` | App refuses to boot (`JwtUtil` constructor throws) | `jwt-secret` | For local dev, generate your own instead (`openssl rand -base64 48`) — it doesn't need to match production, since local tokens only need to validate against your own local instance. |
| `DB_PASSWORD` | Flyway/Hibernate can't connect, app fails to boot | `db-password` | `gcloud secrets versions access latest --secret=db-password --project=ecstatic-seeker-474703-d4`. **Note:** `application.yml` points at the live production Cloud SQL instance (`legalpro-postgres-au`), not a local DB — connecting locally means Flyway runs any pending migrations against production. |
| `GOOGLE_API_KEY` | `AddressController` (Google Places/Maps lookups) fails at request time, not at boot | `google-api-key` | `gcloud secrets versions access latest --secret=google-api-key --project=ecstatic-seeker-474703-d4` |

If `gcloud` isn't authenticated locally, run `gcloud auth login` first (a
one-time browser login with your own Google account — separate from the
Application Default Credentials login mentioned below).

## Required for file storage (GCS) and Firebase

Not read via a named env var — these use Google's **Application Default
Credentials** (ADC), resolved automatically by the Google client libraries.
Pick one:

- Run `gcloud auth application-default login` once on your machine (uses your
  own Google identity — you need Storage + Firestore access on the project), or
- Set `GOOGLE_APPLICATION_CREDENTIALS=<path to a service-account JSON key file>`
  with those same permissions.

Without either, file upload/download endpoints and Firebase-backed features
fail at request time (app still boots fine).

## Required only for "Generate Advice" (Claude AI letter drafting)

These four are non-secret identifiers (not API keys), so they're already sitting
in plain text in `deploy.yml`'s `--set-env-vars` line — copy them directly:

| Variable | Value (from `deploy.yml`) |
|---|---|
| `ANTHROPIC_FEDERATION_RULE_ID` | `fdrl_01JwLkiPWZjDCktuixq3CVVr` |
| `ANTHROPIC_ORGANIZATION_ID` | `2c3f3577-55a7-44a0-ae47-d32f9310d578` |
| `ANTHROPIC_SERVICE_ACCOUNT_ID` | `svac_01Xmh9AW7pzckkSYf3FDN8qb` |
| `ANTHROPIC_WORKSPACE_ID` | `wrkspc_01BFAcJJ8xvNvjSpfbKgZnEk` |
| `ANTHROPIC_IDENTITY_TOKEN_FILE` | Any file path, e.g. `/tmp/anthropic-identity-token.jwt` — but see below, setting this alone won't make generation work locally. |

**These four IDs alone are not enough to generate letters locally.** In
production, `AnthropicIdentityTokenRefresher` keeps `ANTHROPIC_IDENTITY_TOKEN_FILE`
populated with a fresh Google-signed identity token fetched from the **Cloud
Run metadata server** — which doesn't exist on your laptop. Setting these vars
locally will get you past app startup, but the actual "Generate Advice" call
will still fail with a clear "AI letter drafting is not available" error.
Everything else in the app works fine regardless — this is a known, permanent
local-dev gap, not a missing config value.

## Stripe

| Variable | What it is | Secret Manager name / value |
|---|---|---|
| `STRIPE_SECRET_KEY` | Test-mode secret key | `stripe-secret-key` — `gcloud secrets versions access latest --secret=stripe-secret-key --project=ecstatic-seeker-474703-d4` |
| `STRIPE_PUBLISHABLE_KEY` | Test-mode publishable key (not secret) | Already plain text in `deploy.yml`: `pk_test_51SQ9eSCc2zUKTpNVRXn1Nt1OTW7TRuCykp76d7dHpFcqWyoDYz4WW4rABmTq8iHxVEKCBvgKanIxNbTTKObt05y300wFWAEDVI` |
| `STRIPE_WEBHOOK_SECRET` | Signing secret for the webhook endpoint | `stripe-webhook-secret` in Secret Manager — but for **local webhook testing** use the Stripe CLI instead (`stripe listen --forward-to localhost:8080/api/stripe/webhook`), which prints its own local signing secret; the production one won't verify local requests anyway. |

## Email

| Variable | Secret Manager name | Fetch with |
|---|---|---|
| `SENDGRID_API_KEY` | `sendgrid-api-key` | `gcloud secrets versions access latest --secret=sendgrid-api-key --project=ecstatic-seeker-474703-d4` |

## Optional (safe defaults already in code)

| Variable | Default | Notes |
|---|---|---|
| `PORT` | `8080` | |
| `FRONTEND_BASE_URL` | `https://bossjustice.com.au` | Used to build links inside generated documents/emails. Set to `http://localhost:3000` locally if you want those links to point at your local frontend instead of production. |

## Setting these in IntelliJ

Run/Debug Configurations → your `AccountServiceApplication` config →
**Environment variables** field → paste `KEY=value` pairs separated by `;`,
or click the folder icon to add them one at a time.

`.env.example` in this directory lists every variable name above as a
template — copy it to `.env` (already gitignored) to keep your own values
somewhere durable instead of only pasted into an IDE dialog.
