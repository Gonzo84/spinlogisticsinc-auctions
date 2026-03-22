-- US Market Adaptation: VAT → EIN, Country → State
ALTER TABLE app.companies RENAME COLUMN vat_id TO ein;
ALTER TABLE app.companies RENAME COLUMN country TO state;
ALTER TABLE app.companies ADD COLUMN IF NOT EXISTS entity_type VARCHAR(20) NOT NULL DEFAULT '';

-- Update default currency for new users
COMMENT ON COLUMN app.companies.ein IS 'US Employer Identification Number (XX-XXXXXXX)';
COMMENT ON COLUMN app.companies.state IS 'US state code (2-letter, e.g. NY, CA, TX)';
COMMENT ON COLUMN app.companies.entity_type IS 'US business entity type (LLC, C-Corp, S-Corp, LP, Sole Prop)';
