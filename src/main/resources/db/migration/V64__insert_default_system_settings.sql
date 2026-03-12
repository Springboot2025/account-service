-- V64__insert_default_system_settings.sql

INSERT INTO system_settings (
    settings
)
VALUES (
'{
  "notifications": {
    "emailNotifications": {
      "title": "Email Notifications",
      "description": "Send email alerts for important events",
      "value": true
    },
    "smsNotifications": {
      "title": "SMS Notifications",
      "description": "Send SMS for urgent notifications",
      "value": true
    },
    "weeklyReports": {
      "title": "Weekly Reports",
      "description": "Receive weekly summary emails",
      "value": false
    }
  },
  "security": {
    "twoFactorAuthentication": {
      "title": "Two-Factor Authentication",
      "description": "Require 2FA for all admin logins",
      "value": true
    },
    "sessionTimeoutMinutes": {
      "title": "Session Timeout",
      "description": "Auto-logout after inactivity (minutes)",
      "value": 30
    },
    "maxLoginAttempts": {
      "title": "Login Attempts",
      "description": "Max failed login attempts before lockout",
      "value": 5
    }
  },
  "platform": {
    "platformName": {
      "title": "Platform Name",
      "description": "Display name for your platform",
      "value": "Boss Law"
    },
    "supportEmail": {
      "title": "Support Email",
      "description": "Contact email for user support",
      "value": "support@bosslaw.com"
    },
    "timezone": {
      "title": "Timezone",
      "description": "Default timezone for the platform",
      "value": "Australia/Sydney"
    }
  }
}'::jsonb
);