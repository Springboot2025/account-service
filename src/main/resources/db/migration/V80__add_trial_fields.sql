-- Free trial support: a new subscription starts active immediately with a
-- deferred first charge, instead of being charged the full plan price today.
ALTER TABLE subscriptions ADD COLUMN trial_days INT NOT NULL DEFAULT 30;

ALTER TABLE user_subscriptions
    ADD COLUMN is_trial BOOLEAN NOT NULL DEFAULT FALSE,
    -- Sticky even across cancel + re-subscribe (the same row is reused in place,
    -- see PayWayPaymentService.activateSubscription) -- one free trial per account, ever.
    ADD COLUMN has_used_trial BOOLEAN NOT NULL DEFAULT FALSE;

-- Which reminder emails have already gone out for a trial subscription. A row
-- per (subscription, days_before) sent, not two fixed boolean columns, so the
-- admin-configurable reminder schedule (system_settings.payments) can have any
-- number of entries. The unique constraint also doubles as the guard against
-- sending the same reminder twice if more than one app instance runs the job
-- at once (insert-before-send; a conflict means someone already claimed it).
CREATE TABLE trial_reminder_sends (
    id BIGSERIAL PRIMARY KEY,
    user_subscription_uuid UUID NOT NULL,
    days_before INT NOT NULL,
    sent_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (user_subscription_uuid, days_before)
);

-- Records the refund of the small card-verification charge made at trial
-- signup (see PayWayClient.refundTransaction / SubscriptionInvoiceType.VERIFICATION).
ALTER TABLE payway_transactions
    ADD COLUMN refunded_at TIMESTAMP,
    ADD COLUMN refund_transaction_id BIGINT;
