-- Campus Repair Ticket System - MySQL 8.0+ schema
-- Run this script manually against MySQL during development.
-- Production database credentials and schema management must be configured separately.

CREATE DATABASE IF NOT EXISTS campus_repair
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_0900_ai_ci;

USE campus_repair;

CREATE TABLE IF NOT EXISTS sys_user (
    id              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    username        VARCHAR(64)  NOT NULL,
    password_hash   VARCHAR(100) NOT NULL,
    name            VARCHAR(64)  NOT NULL,
    phone           VARCHAR(20)  NULL,
    enabled         TINYINT(1)   NOT NULL DEFAULT 1,
    created_at      DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at      DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    CONSTRAINT uk_sys_user_username UNIQUE (username),
    CONSTRAINT uk_sys_user_phone UNIQUE (phone),
    CONSTRAINT ck_sys_user_enabled CHECK (enabled IN (0, 1))
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS sys_role (
    id              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    role_code       VARCHAR(32) NOT NULL,
    role_name       VARCHAR(64) NOT NULL,
    created_at      DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    CONSTRAINT uk_sys_role_code UNIQUE (role_code)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS sys_user_role (
    user_id         BIGINT UNSIGNED NOT NULL,
    role_id         BIGINT UNSIGNED NOT NULL,
    created_at      DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (user_id, role_id),
    CONSTRAINT fk_user_role_user FOREIGN KEY (user_id) REFERENCES sys_user (id) ON DELETE RESTRICT,
    CONSTRAINT fk_user_role_role FOREIGN KEY (role_id) REFERENCES sys_role (id) ON DELETE RESTRICT,
    KEY idx_user_role_role_user (role_id, user_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS building (
    id              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    name            VARCHAR(100) NOT NULL,
    address         VARCHAR(200) NULL,
    enabled         TINYINT(1) NOT NULL DEFAULT 1,
    sort_order      INT NOT NULL DEFAULT 0,
    created_at      DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at      DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    CONSTRAINT uk_building_name UNIQUE (name),
    CONSTRAINT ck_building_enabled CHECK (enabled IN (0, 1)),
    KEY idx_building_enabled_sort (enabled, sort_order)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS repair_category (
    id              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    name            VARCHAR(50) NOT NULL,
    description     VARCHAR(200) NULL,
    enabled         TINYINT(1) NOT NULL DEFAULT 1,
    sort_order      INT NOT NULL DEFAULT 0,
    created_at      DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at      DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    CONSTRAINT uk_repair_category_name UNIQUE (name),
    CONSTRAINT ck_repair_category_enabled CHECK (enabled IN (0, 1)),
    KEY idx_repair_category_enabled_sort (enabled, sort_order)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

-- Optional equipment registry. A ticket may omit asset_id and provide room/location text instead.
CREATE TABLE IF NOT EXISTS repair_asset (
    id              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    building_id     BIGINT UNSIGNED NOT NULL,
    asset_code      VARCHAR(64) NULL,
    name            VARCHAR(100) NOT NULL,
    location        VARCHAR(100) NULL,
    enabled         TINYINT(1) NOT NULL DEFAULT 1,
    created_at      DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at      DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    CONSTRAINT fk_repair_asset_building FOREIGN KEY (building_id) REFERENCES building (id) ON DELETE RESTRICT,
    CONSTRAINT ck_repair_asset_enabled CHECK (enabled IN (0, 1)),
    CONSTRAINT uk_repair_asset_code UNIQUE (building_id, asset_code),
    KEY idx_repair_asset_building_enabled (building_id, enabled)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS ticket (
    id                      BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    ticket_no               VARCHAR(32) NOT NULL,
    reporter_id             BIGINT UNSIGNED NOT NULL,
    assignee_id             BIGINT UNSIGNED NULL,
    category_id             BIGINT UNSIGNED NOT NULL,
    building_id             BIGINT UNSIGNED NOT NULL,
    asset_id                BIGINT UNSIGNED NULL,
    category_name_snapshot  VARCHAR(50) NOT NULL,
    building_name_snapshot  VARCHAR(100) NOT NULL,
    asset_name_snapshot     VARCHAR(100) NULL,
    title                   VARCHAR(100) NOT NULL,
    description             TEXT NOT NULL,
    room                    VARCHAR(100) NOT NULL,
    priority                VARCHAR(16) NOT NULL DEFAULT 'NORMAL',
    status                  VARCHAR(32) NOT NULL DEFAULT 'PENDING_ASSIGNMENT',
    resolution              TEXT NULL,
    version                 INT UNSIGNED NOT NULL DEFAULT 0,
    sla_due_at              DATETIME(3) NULL,
    created_at              DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at              DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    CONSTRAINT uk_ticket_ticket_no UNIQUE (ticket_no),
    CONSTRAINT fk_ticket_reporter FOREIGN KEY (reporter_id) REFERENCES sys_user (id) ON DELETE RESTRICT,
    CONSTRAINT fk_ticket_assignee FOREIGN KEY (assignee_id) REFERENCES sys_user (id) ON DELETE RESTRICT,
    CONSTRAINT fk_ticket_category FOREIGN KEY (category_id) REFERENCES repair_category (id) ON DELETE RESTRICT,
    CONSTRAINT fk_ticket_building FOREIGN KEY (building_id) REFERENCES building (id) ON DELETE RESTRICT,
    CONSTRAINT fk_ticket_asset FOREIGN KEY (asset_id) REFERENCES repair_asset (id) ON DELETE RESTRICT,
    CONSTRAINT ck_ticket_priority CHECK (priority IN ('LOW', 'NORMAL', 'HIGH', 'URGENT')),
    CONSTRAINT ck_ticket_status CHECK (status IN (
        'PENDING_ASSIGNMENT', 'PENDING_ACCEPTANCE', 'ACCEPTED', 'PROCESSING',
        'PENDING_CONFIRMATION', 'COMPLETED', 'CANCELLED', 'CLOSED'
    )),
    KEY idx_ticket_reporter_created (reporter_id, created_at DESC, id DESC),
    KEY idx_ticket_assignee_status_updated (assignee_id, status, updated_at DESC, id DESC),
    KEY idx_ticket_status_created (status, created_at DESC, id DESC),
    KEY idx_ticket_building_status_created (building_id, status, created_at DESC, id DESC),
    KEY idx_ticket_category_created (category_id, created_at DESC, id DESC),
    KEY idx_ticket_sla_status (status, sla_due_at)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

-- Daily ticket number sequence. The row is incremented atomically inside the ticket creation transaction.
CREATE TABLE IF NOT EXISTS ticket_no_sequence (
    sequence_date   DATE NOT NULL,
    current_value   INT UNSIGNED NOT NULL,
    PRIMARY KEY (sequence_date)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS ticket_history (
    id                  BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    ticket_id           BIGINT UNSIGNED NOT NULL,
    event_version       INT UNSIGNED NOT NULL,
    from_status         VARCHAR(32) NULL,
    to_status           VARCHAR(32) NOT NULL,
    action              VARCHAR(32) NOT NULL,
    operator_id         BIGINT UNSIGNED NOT NULL,
    from_assignee_id    BIGINT UNSIGNED NULL,
    to_assignee_id      BIGINT UNSIGNED NULL,
    remark              VARCHAR(500) NULL,
    created_at          DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    CONSTRAINT uk_ticket_history_version UNIQUE (ticket_id, event_version),
    CONSTRAINT fk_ticket_history_ticket FOREIGN KEY (ticket_id) REFERENCES ticket (id) ON DELETE RESTRICT,
    CONSTRAINT fk_ticket_history_operator FOREIGN KEY (operator_id) REFERENCES sys_user (id) ON DELETE RESTRICT,
    CONSTRAINT fk_ticket_history_from_assignee FOREIGN KEY (from_assignee_id) REFERENCES sys_user (id) ON DELETE RESTRICT,
    CONSTRAINT fk_ticket_history_to_assignee FOREIGN KEY (to_assignee_id) REFERENCES sys_user (id) ON DELETE RESTRICT,
    KEY idx_ticket_history_ticket_time (ticket_id, created_at, id),
    KEY idx_ticket_history_operator_time (operator_id, created_at)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS ticket_attachment (
    id                  BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    ticket_id           BIGINT UNSIGNED NOT NULL,
    uploaded_by         BIGINT UNSIGNED NOT NULL,
    file_type           VARCHAR(16) NOT NULL,
    file_url            VARCHAR(500) NOT NULL,
    original_filename   VARCHAR(255) NULL,
    media_type          VARCHAR(100) NULL,
    size_bytes          BIGINT UNSIGNED NOT NULL,
    sort_order          INT NOT NULL DEFAULT 0,
    created_at          DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    CONSTRAINT fk_ticket_attachment_ticket FOREIGN KEY (ticket_id) REFERENCES ticket (id) ON DELETE RESTRICT,
    CONSTRAINT fk_ticket_attachment_user FOREIGN KEY (uploaded_by) REFERENCES sys_user (id) ON DELETE RESTRICT,
    CONSTRAINT ck_ticket_attachment_type CHECK (file_type IN ('REPORT', 'RESOLUTION')),
    KEY idx_ticket_attachment_ticket_type (ticket_id, file_type, sort_order)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS ticket_rating (
    id              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    ticket_id       BIGINT UNSIGNED NOT NULL,
    reporter_id     BIGINT UNSIGNED NOT NULL,
    score           TINYINT UNSIGNED NOT NULL,
    comment         VARCHAR(500) NULL,
    created_at      DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    CONSTRAINT uk_ticket_rating_ticket UNIQUE (ticket_id),
    CONSTRAINT fk_ticket_rating_ticket FOREIGN KEY (ticket_id) REFERENCES ticket (id) ON DELETE RESTRICT,
    CONSTRAINT fk_ticket_rating_reporter FOREIGN KEY (reporter_id) REFERENCES sys_user (id) ON DELETE RESTRICT,
    CONSTRAINT ck_ticket_rating_score CHECK (score BETWEEN 1 AND 5),
    KEY idx_ticket_rating_reporter_time (reporter_id, created_at)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;
