-- Add Stripe tracking columns to client_invoices
ALTER TABLE client_invoices
ADD COLUMN stripe_session_id VARCHAR(255),
ADD COLUMN stripe_payment_status VARCHAR(50);

-- Optional: helpful index for webhook lookup
CREATE INDEX idx_client_invoices_stripe_session_id
    ON client_invoices(stripe_session_id);
