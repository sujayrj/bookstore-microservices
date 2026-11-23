
CREATE ROLE sujay
    WITH LOGIN
    PASSWORD 'sujay_password';

CREATE DATABASE "catalog-db"
    OWNER sujay;

GRANT ALL PRIVILEGES ON DATABASE "catalog-db" TO sujay;