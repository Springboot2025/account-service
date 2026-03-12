-- V62__insert_default_subscription_plans.sql

INSERT INTO subscriptions (
    plan_name,
    description,
    monthly_price,
    annual_price,
    recommended,
    features
)
VALUES
(
    'Subscription for Individual Lawyers',
    'Perfect for solo practitioners',
    20,
    200,
    FALSE,
    '[
        "Secure central storage of sensitive information",
        "Public profile to access potential clients Victoria wide",
        "Real time notifications and various tools to assist you in communicating with your clients",
        "Diari sing matters to send you reminders of upcoming Court dates",
        "Remain in control of your data and information by ensuring downloading and sharing is not permitted"
    ]'::jsonb
),
(
    'Subscription for Firms',
    'Best for law firms and teams',
    50,
    500,
    TRUE,
    '[
        "Secure central storage of sensitive information",
        "Public profile to access potential clients Victoria wide",
        "Real time notifications and various tools to assist you in communicating with your clients",
        "Minimising time and resources expended on admin by providing access to all your work in an organised fashion",
        "Remain in control of your data and information by ensuring downloading and sharing is not permitted"
    ]'::jsonb
);