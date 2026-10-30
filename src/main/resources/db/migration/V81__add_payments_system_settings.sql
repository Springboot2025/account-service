-- Adds a "payments" section to the existing system_settings row (same
-- {title, description, value} shape as "notifications"/"security"/"platform"),
-- read/written generically by the existing GET/PUT /api/superadmin/system-settings.
-- autoRenewEnabled is NOT included here -- that stays controlled by the
-- PAYWAY_RENEWAL_AUTO_ENABLED infra flag (GCP secret), surfaced read-only
-- elsewhere, not editable through this endpoint.
UPDATE system_settings
SET settings = settings || '{
  "payments": {
    "trialReminderEmailsEnabled": {
      "title": "Trial Reminder Emails",
      "description": "Email trial subscribers before their card is first charged",
      "value": true
    },
    "trialReminderDaysBeforeCharge": {
      "title": "Trial Reminder Schedule",
      "description": "Days before the first charge to send a reminder email (e.g. [5, 1])",
      "value": [5, 1]
    },
    "verificationAmount": {
      "title": "Card Verification Amount (AUD)",
      "description": "Small refundable charge used to verify a card at trial signup",
      "value": 1.00
    }
  }
}'::jsonb
WHERE removed_at IS NULL;
