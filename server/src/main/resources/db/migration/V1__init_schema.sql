CREATE TABLE IF NOT EXISTS users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    email VARCHAR(320) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    role VARCHAR(20) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS activities (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    external_activity_id VARCHAR(100) NOT NULL,
    activity_type VARCHAR(50) NOT NULL,
    activity_name VARCHAR(200) NOT NULL,
    description VARCHAR(2000),
    activity_date_time TIMESTAMP NOT NULL,
    activity_month VARCHAR(7) NOT NULL,
    distance_km DOUBLE,
    moving_time_seconds INT,
    elapsed_time_seconds INT,
    average_speed DOUBLE,
    elevation_gain DOUBLE,
    calories INT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_activities_user_external UNIQUE (user_id, external_activity_id),
    CONSTRAINT fk_activities_user FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE INDEX IF NOT EXISTS idx_activities_user_month ON activities(user_id, activity_month);
CREATE INDEX IF NOT EXISTS idx_activities_external_id ON activities(external_activity_id);

CREATE TABLE IF NOT EXISTS album_projects (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    album_month VARCHAR(7) NOT NULL,
    title VARCHAR(150) NOT NULL,
    subtitle VARCHAR(300),
    monthly_review VARCHAR(4000),
    status VARCHAR(30) NOT NULL,
    book_uid VARCHAR(100),
    book_external_ref VARCHAR(120),
    book_status VARCHAR(30) NOT NULL,
    book_finalization_pending BOOLEAN NOT NULL DEFAULT FALSE,
    book_generated_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_album_projects_user FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE TABLE IF NOT EXISTS album_activities (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    album_project_id BIGINT NOT NULL,
    activity_id BIGINT NOT NULL,
    memo VARCHAR(2000),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_album_activity_album_activity UNIQUE (album_project_id, activity_id),
    CONSTRAINT fk_album_activities_album_project FOREIGN KEY (album_project_id) REFERENCES album_projects(id),
    CONSTRAINT fk_album_activities_activity FOREIGN KEY (activity_id) REFERENCES activities(id)
);

CREATE TABLE IF NOT EXISTS activity_photos (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    album_activity_id BIGINT NOT NULL,
    original_file_name VARCHAR(260) NOT NULL,
    stored_file_name VARCHAR(260) NOT NULL,
    content_type VARCHAR(120) NOT NULL,
    file_size BIGINT NOT NULL,
    storage_path VARCHAR(1000) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_activity_photos_album_activity FOREIGN KEY (album_activity_id) REFERENCES album_activities(id)
);

CREATE INDEX IF NOT EXISTS idx_activity_photos_album_activity ON activity_photos(album_activity_id);

CREATE TABLE IF NOT EXISTS album_orders (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    album_project_id BIGINT NOT NULL,
    order_uid VARCHAR(120),
    external_ref VARCHAR(120) NOT NULL,
    request_payload VARCHAR(8000) NOT NULL,
    status VARCHAR(30) NOT NULL,
    last_error_message VARCHAR(500),
    remote_order_status_code INT,
    remote_order_status_display VARCHAR(120),
    remote_ordered_at TIMESTAMP,
    last_webhook_delivery_id VARCHAR(120),
    last_webhook_event_type VARCHAR(120),
    last_webhook_received_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_album_orders_album_external_ref UNIQUE (album_project_id, external_ref),
    CONSTRAINT uk_album_orders_order_uid UNIQUE (order_uid),
    CONSTRAINT fk_album_orders_album_project FOREIGN KEY (album_project_id) REFERENCES album_projects(id)
);

CREATE INDEX IF NOT EXISTS idx_album_orders_album_project ON album_orders(album_project_id);
CREATE INDEX IF NOT EXISTS idx_album_project_created_at ON album_orders(album_project_id, created_at DESC);

