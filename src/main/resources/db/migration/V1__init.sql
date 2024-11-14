-- V1__init.sql

-- Account
CREATE TABLE IF NOT EXISTS `account` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `uid` VARCHAR(255),
    `provider` VARCHAR(255),
    `provider_id` VARCHAR(255),
    `display_name` VARCHAR(255),
    `email` VARCHAR(255),
    `profile_image_url` VARCHAR(255),
    `access_token` VARCHAR(255),
    `refresh_token` VARCHAR(255),
    `access_token_fetched_at` DATETIME(6),
    `last_login_at` DATETIME(6),
    `has_bookmark` BOOLEAN DEFAULT FALSE,
    `created_at` DATETIME(6),
    `updated_at` DATETIME(6),
    `default_signature_id` BIGINT,
    PRIMARY KEY (`id`)
    UNIQUE INDEX `idx_uid` (`uid`);
) ENGINE=InnoDB;

-- account_marketing_email_timestamps
CREATE TABLE IF NOT EXISTS `account_marketing_email_timestamps` (
    `account_id` BIGINT NOT NULL,
    `timestamp` DATETIME(6),
    `marketing_email` BIT NOT NULL,
    PRIMARY KEY (`account_id`, `marketing_email`),
    FOREIGN KEY (`account_id`) REFERENCES `member_preference` (`id`)
) ENGINE=InnoDB;

-- account_security_email_timestamps
CREATE TABLE IF NOT EXISTS `account_security_email_timestamps` (
    `account_id` BIGINT NOT NULL,
    `timestamp` DATETIME(6),
    `security_email` BIT NOT NULL,
    PRIMARY KEY (`account_id`, `security_email`),
    FOREIGN KEY (`account_id`) REFERENCES `member_preference` (`id`)
) ENGINE=InnoDB;

-- accounts_watch_notifications
CREATE TABLE IF NOT EXISTS `accounts_watch_notifications` (
    `member_id` BIGINT NOT NULL,
    `account_uid` VARCHAR(255) NOT NULL,
    `notification_setting` ENUM('INBOX', 'IMPORTANT', 'OFF'),
    PRIMARY KEY (`member_id`, `account_uid`),
    FOREIGN KEY (`member_id`) REFERENCES `member_preference` (`id`)
) ENGINE=InnoDB;

-- Email Template
CREATE TABLE IF NOT EXISTS `email_template` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `account_id` BIGINT,
    `body` TEXT,
    `subject` VARCHAR(255),
    `template_name` VARCHAR(255),
    PRIMARY KEY (`id`),
    INDEX `idx_account_id` (`account_id`)
    FOREIGN KEY (`account_id`) REFERENCES `account` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB;

-- bookmark
CREATE TABLE IF NOT EXISTS `user_bookmark` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `account_id` BIGINT,
    `query` VARCHAR(255),
    `title` VARCHAR(255),
    `icon` VARCHAR(255),
    PRIMARY KEY (`id`),
    INDEX `idx_account_id` (`account_id`);
    FOREIGN KEY (`account_id`) REFERENCES `account` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB;

-- Signature
CREATE TABLE IF NOT EXISTS `signature` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `created_at` DATETIME(6),
    `updated_at` DATETIME(6),
    `account_id` BIGINT,
    `title` VARCHAR(255),
    `content` TINYTEXT,
    PRIMARY KEY (`id`),
    INDEX `idx_account_id` (`account_id`);
    FOREIGN KEY (`account_id`) REFERENCES `account` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB;

-- pin
CREATE TABLE IF NOT EXISTS `pin` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `account_id` BIGINT NOT NULL,
    PRIMARY KEY (`id`),
    INDEX `idx_account_id` (`account_id`);
    FOREIGN KEY (`account_id`) REFERENCES `account` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB;

-- PinEmailAddresses
CREATE TABLE IF NOT EXISTS `pin_email_addresses` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `pin_id` BIGINT NOT NULL,
    `email_address` VARCHAR(255),
    PRIMARY KEY (`id`),
    FOREIGN KEY (`pin_id`) REFERENCES `pin` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB;

-- Member
CREATE TABLE IF NOT EXISTS `member` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `display_name` VARCHAR(255),
    `email` VARCHAR(255),
    `member_name` VARCHAR(255),
    `profile_image_url` VARCHAR(255),
    `primary_uid` VARCHAR(255),
    `deleted_at` DATETIME(6),
    `created_at` DATETIME(6),
    `updated_at` DATETIME(6),
    PRIMARY KEY (`id`)
    UNIQUE INDEX `idx_primary_uid` (`primary_uid`);
) ENGINE=InnoDB;

-- MemberAccount
CREATE TABLE IF NOT EXISTS `member_account` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `member_id` BIGINT,
    `account_id` BIGINT,
    PRIMARY KEY (`id`),
    FOREIGN KEY (`member_id`) REFERENCES `member` (`id`) ON DELETE CASCADE,
    FOREIGN KEY (`account_id`) REFERENCES `account` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB;

-- Member Preference
CREATE TABLE IF NOT EXISTS `member_preference` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `alert_sound` VARCHAR(255),
    `close_window` INTEGER,
    `density` ENUM('COMPACT', 'COZY'),
    `language` VARCHAR(255),
    `marketing_emails` BOOLEAN,
    `security_emails` BOOLEAN,
    `theme` ENUM('DARK', 'LIGHT', 'SYSTEM'),
    `member_id` BIGINT,
    PRIMARY KEY (`id`),
    UNIQUE INDEX `UK756oaax8a9pl827lg5g2b8d3n` (`member_id`),
    FOREIGN KEY (`member_id`) REFERENCES `member` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB;

-- FCM Token
CREATE TABLE IF NOT EXISTS `fcm_token` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `account_id` BIGINT,
    `fcm_token` VARCHAR(255),
    `machine_uuid` VARCHAR(255),
    PRIMARY KEY (`id`),
    FOREIGN KEY (`account_id`) REFERENCES `account` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB;

-- Pub Sub History
CREATE TABLE IF NOT EXISTS `pub_sub_history` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `history_id` DECIMAL(38,0),
    `account_id` BIGINT,
    PRIMARY KEY (`id`),
    FOREIGN KEY (`account_id`) REFERENCES `account` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB;

-- Verification Email
CREATE TABLE IF NOT EXISTS `verification_email` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `thread_id` VARCHAR(255),
    `message_id` VARCHAR(255),
    `codes` VARCHAR(255),
    `links` VARCHAR(255),
    `uuid` VARCHAR(255),
    `account_id` BIGINT,
    `created_at` DATETIME(6),
    PRIMARY KEY (`id`),
    FOREIGN KEY (`account_id`) REFERENCES `account`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB;

-- SharedEmail
CREATE TABLE IF NOT EXISTS `shared_email` (
    `id` BINARY(16) PRIMARY KEY,
    `access` ENUM('RESTRICTED', 'PUBLIC'),
    `data_id` VARCHAR(255),
    `shared_data_type` ENUM('MESSAGE', 'THREAD'),
    `owner_id` BIGINT,
    `can_editor_edit_permission` BOOLEAN,
    `can_viewer_view_tool_menu` BOOLEAN,
    `created_at` DATETIME(6),
    `updated_at` DATETIME(6)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS `shared_email_invitee` (
    `shared_email_id` BINARY(16),
    `invitee_email` VARCHAR(255),
    `permission` ENUM('EDITOR', 'COMMENTER', 'VIEWER', 'PUBLIC_VIEWER'),
    PRIMARY KEY (`shared_email_id`),
    FOREIGN KEY (`shared_email_id`) REFERENCES `shared_email`(`id`)
) ENGINE=InnoDB;
