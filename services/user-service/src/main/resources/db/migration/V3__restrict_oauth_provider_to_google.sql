ALTER TABLE oauth_accounts
    ADD CONSTRAINT chk_oauth_accounts_provider_google
        CHECK (provider = 'GOOGLE');
