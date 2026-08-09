-- Hibernate maps @Column(length=3) to VARCHAR; align CHAR columns from early migrations.

ALTER TABLE booking
    ALTER COLUMN currency_code TYPE VARCHAR(3) USING currency_code::varchar;

ALTER TABLE payment
    ALTER COLUMN currency_code TYPE VARCHAR(3) USING currency_code::varchar;
