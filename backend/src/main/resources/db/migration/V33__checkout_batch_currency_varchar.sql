-- Hibernate maps @Column(length=3) to VARCHAR; V29 used CHAR(3) for checkout_batch.

ALTER TABLE checkout_batch
    ALTER COLUMN currency_code TYPE VARCHAR(3) USING currency_code::varchar;
