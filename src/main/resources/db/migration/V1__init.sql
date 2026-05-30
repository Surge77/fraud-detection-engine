CREATE TABLE audit_log (
    id              BIGSERIAL PRIMARY KEY,
    transaction_id  VARCHAR(64) UNIQUE NOT NULL,
    account_id      VARCHAR(64) NOT NULL,
    merchant_id     VARCHAR(64),
    amount          NUMERIC(15, 2),
    currency        VARCHAR(10),
    location        VARCHAR(100),
    risk_score      INT,
    decision        VARCHAR(10),
    reasons         JSONB,
    created_at      TIMESTAMPTZ DEFAULT NOW()
);

-- Nightly report reads previous day's rows by created_at.
CREATE INDEX idx_audit_log_created_at ON audit_log (created_at);
CREATE INDEX idx_audit_log_account_id ON audit_log (account_id);

CREATE TABLE account_limits (
    account_id   VARCHAR(64) PRIMARY KEY,
    daily_limit  NUMERIC(15, 2) NOT NULL
);

CREATE TABLE fraud_reports (
    id                   BIGSERIAL PRIMARY KEY,
    report_date          DATE,
    total_transactions   INT,
    flagged_count        INT,
    flag_rate            NUMERIC(5, 2),
    avg_risk_score       NUMERIC(5, 2),
    top_risky_merchants  JSONB,
    created_at           TIMESTAMPTZ DEFAULT NOW()
);

CREATE UNIQUE INDEX idx_fraud_reports_date ON fraud_reports (report_date);
