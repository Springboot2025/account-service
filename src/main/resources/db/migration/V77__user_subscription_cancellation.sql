-- Cancelling a subscription: renewals stop, access continues until renews_at.
ALTER TABLE user_subscriptions
    ADD COLUMN cancelled_at TIMESTAMP,
    ADD COLUMN cancel_reason TEXT;
