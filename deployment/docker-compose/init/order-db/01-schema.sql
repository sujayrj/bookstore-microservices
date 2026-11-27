
CREATE ROLE sujay
    WITH LOGIN
    PASSWORD 'sujay_password';

CREATE DATABASE "order-db"
    OWNER sujay;

GRANT ALL PRIVILEGES ON DATABASE "order-db" TO sujay;