-- =============================================================================
-- V001: Create core user-service tables
-- =============================================================================
-- Covers: users, companies, kyc_records, deposits
-- Schema: app (application schema, separate from system/public)
-- =============================================================================

CREATE SCHEMA IF NOT EXISTS app;

-- -----------------------------------------------------------------------------
-- Users
-- -----------------------------------------------------------------------------
CREATE TABLE app.users (
    id              UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    keycloak_id     VARCHAR(255) NOT NULL UNIQUE,
    account_type    VARCHAR(20)  NOT NULL,
    email           VARCHAR(255) NOT NULL UNIQUE,
    phone           VARCHAR(50),
    first_name      VARCHAR(100) NOT NULL,
    last_name       VARCHAR(100) NOT NULL,
    language        VARCHAR(10)  DEFAULT 'en',
    currency        VARCHAR(3)   DEFAULT 'EUR',
    status          VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    deposit_status  VARCHAR(30)  DEFAULT 'NONE',
    created_at      TIMESTAMPTZ  DEFAULT NOW(),
    updated_at      TIMESTAMPTZ  DEFAULT NOW()
);

-- -----------------------------------------------------------------------------
-- Companies (one-to-one with BUSINESS users)
-- -----------------------------------------------------------------------------
CREATE TABLE app.companies (
    id              UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         UUID         NOT NULL REFERENCES app.users(id),
    company_name    VARCHAR(255) NOT NULL,
    registration_no VARCHAR(100),
    vat_id          VARCHAR(50),
    country         VARCHAR(3)   NOT NULL,
    address         TEXT,
    city            VARCHAR(100),
    postal_code     VARCHAR(20),
    verified        BOOLEAN      DEFAULT FALSE,
    created_at      TIMESTAMPTZ  DEFAULT NOW()
);

-- -----------------------------------------------------------------------------
-- KYC Records (identity verification attempts)
-- -----------------------------------------------------------------------------
CREATE TABLE app.kyc_records (
    id              UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         UUID         NOT NULL REFERENCES app.users(id),
    provider        VARCHAR(50)  NOT NULL,
    status          VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    check_id        VARCHAR(255),
    completed_at    TIMESTAMPTZ,
    created_at      TIMESTAMPTZ  DEFAULT NOW()
);

-- -----------------------------------------------------------------------------
-- Deposits (security deposits for high-value bidding)
-- -----------------------------------------------------------------------------
CREATE TABLE app.deposits (
    id                  UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id             UUID          NOT NULL REFERENCES app.users(id),
    amount              DECIMAL(10,2) NOT NULL DEFAULT 200.00,
    currency            VARCHAR(3)    DEFAULT 'EUR',
    paid_at             TIMESTAMPTZ,
    refund_requested_at TIMESTAMPTZ,
    refunded_at         TIMESTAMPTZ,
    psp_reference       VARCHAR(255),
    created_at          TIMESTAMPTZ   DEFAULT NOW()
);

-- -----------------------------------------------------------------------------
-- Indexes
-- -----------------------------------------------------------------------------
CREATE INDEX idx_users_keycloak  ON app.users(keycloak_id);
CREATE INDEX idx_users_email     ON app.users(email);
CREATE INDEX idx_users_status    ON app.users(status);
CREATE INDEX idx_companies_user  ON app.companies(user_id);
CREATE INDEX idx_companies_vat   ON app.companies(vat_id);
CREATE INDEX idx_kyc_user        ON app.kyc_records(user_id);
CREATE INDEX idx_kyc_status      ON app.kyc_records(user_id, status);
CREATE INDEX idx_deposits_user   ON app.deposits(user_id);
CREATE INDEX idx_deposits_active ON app.deposits(user_id) WHERE paid_at IS NOT NULL AND refunded_at IS NULL;
