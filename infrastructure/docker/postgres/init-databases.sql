-- Initialize all service databases
-- Each microservice gets its own database for isolation

CREATE DATABASE keycloak;
CREATE DATABASE auction_engine;
CREATE DATABASE auction_catalog;
CREATE DATABASE auction_users;
CREATE DATABASE auction_payments;
CREATE DATABASE auction_notifications;
CREATE DATABASE auction_media;
CREATE DATABASE auction_sellers;
CREATE DATABASE auction_brokers;
CREATE DATABASE auction_analytics;
CREATE DATABASE auction_compliance;
CREATE DATABASE auction_co2;
CREATE DATABASE auction_search;
CREATE DATABASE auction_gateway;

-- Grant privileges to dev user on all databases
DO $$
DECLARE
    db_name TEXT;
BEGIN
    FOR db_name IN
        SELECT unnest(ARRAY[
            'keycloak', 'auction_engine', 'auction_catalog', 'auction_users',
            'auction_payments', 'auction_notifications', 'auction_media',
            'auction_sellers', 'auction_brokers', 'auction_analytics',
            'auction_compliance', 'auction_co2', 'auction_search', 'auction_gateway'
        ])
    LOOP
        EXECUTE format('GRANT ALL PRIVILEGES ON DATABASE %I TO dev', db_name);
    END LOOP;
END $$;
