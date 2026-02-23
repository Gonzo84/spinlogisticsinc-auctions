-- ==========================================================================
-- V003: Create the invoices table
-- ==========================================================================
-- Stores invoice records for both buyer and seller. Each completed
-- payment generates a buyer invoice (purchase) and a seller invoice
-- (self-billing / credit note).
-- ==========================================================================

CREATE TABLE app.invoices (
    id              UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    payment_id      UUID        NOT NULL REFERENCES app.payments(id),
    invoice_number  TEXT        NOT NULL,
    type            TEXT        NOT NULL,
    pdf_url         TEXT,
    issued_at       TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Index for payment-scoped queries (invoices for a payment)
CREATE INDEX idx_invoices_payment ON app.invoices(payment_id);

-- Unique constraint on invoice number (globally unique)
CREATE UNIQUE INDEX idx_invoices_number ON app.invoices(invoice_number);

-- Index for type-based queries
CREATE INDEX idx_invoices_type ON app.invoices(type);
