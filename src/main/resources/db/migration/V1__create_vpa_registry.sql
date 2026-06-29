CREATE TABLE vpa_registry (
    vpa            VARCHAR(100)  PRIMARY KEY,
    account_id     UUID          NOT NULL,
    account_holder VARCHAR(100)  NOT NULL,
    created_at     TIMESTAMP     NOT NULL DEFAULT NOW()
);
