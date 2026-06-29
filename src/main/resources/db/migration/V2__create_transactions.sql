CREATE TABLE transactions (
    transaction_id UUID          PRIMARY KEY,
    payer_vpa      VARCHAR(100)  NOT NULL REFERENCES vpa_registry(vpa),
    payee_vpa      VARCHAR(100)  NOT NULL REFERENCES vpa_registry(vpa),
    amount_paise   BIGINT        NOT NULL,
    status         VARCHAR(50)   NOT NULL,
    remarks        VARCHAR(50),
    created_at     TIMESTAMP     NOT NULL DEFAULT NOW(),
    updated_at     TIMESTAMP     NOT NULL DEFAULT NOW(),
    CONSTRAINT amount_positive CHECK (amount_paise > 0)
);

CREATE INDEX idx_psp_txn_dedup 
  ON transactions (payer_vpa, payee_vpa, amount_paise, created_at)
  WHERE status NOT IN ('FAILED', 'REVERSED');
