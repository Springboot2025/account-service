-- Per-seat monthly price for a plan (NULL = the plan has no seat concept, e.g.
-- Individual). Kept in the DB (not the frontend) so it can be changed without
-- a redeploy -- see lawproject/src/utils/payway.js.
ALTER TABLE subscriptions ADD COLUMN seat_price NUMERIC(10, 2);
UPDATE subscriptions SET seat_price = 10.00 WHERE id = 2; -- Firms plan

-- Additional lawyer seats purchased beyond the firm admin's own seat.
ALTER TABLE user_subscriptions ADD COLUMN seat_count INT NOT NULL DEFAULT 0;
ALTER TABLE payway_transactions ADD COLUMN seat_count INT NOT NULL DEFAULT 0;
ALTER TABLE subscription_invoices ADD COLUMN seat_count INT NOT NULL DEFAULT 0;
