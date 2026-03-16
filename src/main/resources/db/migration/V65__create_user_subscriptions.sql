CREATE TABLE user_subscriptions (
    id BIGSERIAL PRIMARY KEY,
    uuid UUID DEFAULT gen_random_uuid() NOT NULL UNIQUE,
    user_uuid UUID NOT NULL,
    user_type VARCHAR(20) NOT NULL, -- INDIVIDUAL | FIRM
    plan_id BIGINT NOT NULL,
    status SMALLINT DEFAULT 0 NOT NULL, -- 0 inactive, 1 active, 2 cancelled
    plan_duration VARCHAR(20) NOT NULL, -- monthly | yearly
    start_date TIMESTAMP,
    renews_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP
);

CREATE INDEX idx_user_subscriptions_user_uuid
ON user_subscriptions(user_uuid);

CREATE INDEX idx_user_subscriptions_status
ON user_subscriptions(status);

CREATE INDEX idx_user_subscriptions_plan_id
ON user_subscriptions(plan_id);