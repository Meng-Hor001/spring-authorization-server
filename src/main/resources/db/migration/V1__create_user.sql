CREATE TABLE IF NOT EXISTS authorization_server.app_users (
                                                              id UUID PRIMARY KEY,
                                                              username VARCHAR(100) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE
    );

CREATE TABLE IF NOT EXISTS authorization_server.app_user_roles (
                                                                   user_id UUID NOT NULL
                                                                   REFERENCES authorization_server.app_users(id)
    ON DELETE CASCADE,
    role VARCHAR(50) NOT NULL,
    PRIMARY KEY (user_id, role)
    );