-- 1. User Table
CREATE TABLE `user` (
                        `id` VARCHAR(40) NOT NULL,
                        `name` VARCHAR(40) DEFAULT NULL,
                        `email` VARCHAR(256) DEFAULT NULL,
                        `phone` VARCHAR(256) DEFAULT NULL,
                        `pass` VARCHAR(512) DEFAULT NULL,
                        PRIMARY KEY (`id`)
);

-- 2. Instance Table
CREATE TABLE `inst` (
                        `id` INTEGER NOT NULL,
                        `name` VARCHAR(256) DEFAULT NULL,
                        `port` INT NOT NULL,
                        `log_level` VARCHAR(256) DEFAULT NULL,
                        `os_arch` VARCHAR(256) DEFAULT NULL,
                        `extra_yaml` VARCHAR(5000) DEFAULT NULL,
                        `autostart` INT DEFAULT 1,
                        `autorestart` INT DEFAULT 1,
                        `enabled` INT DEFAULT 1,
                        `pid` INTEGER DEFAULT NULL,
                        `auth` VARCHAR(20),
                        `auth_realm` VARCHAR(512),
                        `auth_service` VARCHAR(512),
                        `auth_issuer` VARCHAR(512),
                        `auth_private_key` TEXT DEFAULT NULL,
                        `auth_public_key` TEXT DEFAULT NULL,
                        `auth_jwks_json` TEXT DEFAULT NULL,
                        `auth_key_type` VARCHAR(20) DEFAULT 'EC',
                        `auth_key_alg` VARCHAR(20) DEFAULT 'ES256',
                        `proxy_username` VARCHAR(256) DEFAULT NULL,
                        `proxy_password` VARCHAR(256) DEFAULT NULL,
                        `proxy_ttl` VARCHAR(256) DEFAULT NULL,
                        `proxy_remoteurl` VARCHAR(512) DEFAULT NULL,
                        `env` VARCHAR(512) DEFAULT NULL,
                        PRIMARY KEY (`id`)
);

-- 3. Repository Table
CREATE TABLE `repo` (
                        `id` INTEGER NOT NULL,
                        `inst_id` INTEGER NOT NULL,
                        `repo_name` VARCHAR(500) NOT NULL,
                        `artifact_count` INT NOT NULL DEFAULT 0,
                        `last_pushed` DATETIME DEFAULT NULL,
                        `description` VARCHAR(1000) DEFAULT NULL,
                        PRIMARY KEY (`id`)
);

-- 4. Global Config Table
CREATE TABLE `global_config` (
                                 `id` INTEGER NOT NULL,
                                 `name` VARCHAR(500) NOT NULL,
                                 `value` VARCHAR(5000) NOT NULL,
                                 PRIMARY KEY (`id`)
);

-- 5. Manifest Table
CREATE TABLE manifest (
                          id INTEGER NOT NULL PRIMARY KEY,
                          inst_id INTEGER NOT NULL,
                          repo_id INTEGER NOT NULL,
                          digest VARCHAR(255) NOT NULL,
                          media_type VARCHAR(500) NOT NULL,
                          `size` INTEGER DEFAULT NULL,
                          os_arch VARCHAR(100) DEFAULT NULL,
                          os VARCHAR(50) DEFAULT NULL,
                          os_version VARCHAR(50) DEFAULT NULL,
                          variant VARCHAR(50) DEFAULT NULL,
                          features VARCHAR(500) DEFAULT NULL,
                          parent_digest VARCHAR(255) DEFAULT NULL,
                          created DATETIME DEFAULT NULL,
                          last_pushed DATETIME DEFAULT NULL,
                          annotations TEXT DEFAULT NULL, -- SQLite handles JSON as TEXT
                          config_digest VARCHAR(255) DEFAULT NULL,
                          pushed_by VARCHAR(255) DEFAULT NULL,
                          push_count INT DEFAULT 0,
                          UNIQUE (inst_id, digest)
);

CREATE INDEX idx_manifest_repo ON manifest (repo_id);
CREATE INDEX idx_manifest_parent ON manifest (parent_digest);
CREATE INDEX idx_manifest_platform ON manifest (os_arch, os, variant);
CREATE INDEX idx_manifest_created ON manifest (created);

-- 6. Artifact Table
CREATE TABLE artifact (
                          id INTEGER NOT NULL PRIMARY KEY,
                          inst_id INTEGER NOT NULL,
                          repo_id INTEGER NOT NULL,
                          repo_name VARCHAR(2000) DEFAULT NULL,
                          manifest_id INTEGER NOT NULL,
                          tag_name VARCHAR(500) NOT NULL,
                          full_name VARCHAR(2500) DEFAULT NULL,
                          created DATETIME DEFAULT NULL,
                          last_pushed DATETIME DEFAULT NULL,
                          last_pulled DATETIME DEFAULT NULL,
                          UNIQUE (inst_id, repo_id, tag_name)
);

CREATE INDEX idx_artifact_manifest ON artifact (manifest_id);
CREATE INDEX idx_artifact_created ON artifact (created);

-- 7. Account Table
CREATE TABLE account (
                         id INTEGER PRIMARY KEY,
                         inst_id INTEGER NOT NULL,
                         username VARCHAR(500) NOT NULL,
                         password VARCHAR(500),
                         hashpw VARCHAR(500) NOT NULL,
                         acl TEXT, -- JSON fields stored as TEXT
                         enabled INT NOT NULL DEFAULT 1,
                         created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                         updated_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                         UNIQUE (inst_id, username)
);

CREATE INDEX idx_inst_id ON account (inst_id);
CREATE INDEX idx_username ON account (username);
CREATE INDEX idx_enabled ON account (enabled);

-- 8. Data Insertion
INSERT INTO `user` (id, name, email, phone, pass) VALUES ('1', 'admin','hello@admin.com','13988888888', '$2a$12$nXPoohJkpNbD1oSxtN0P1uGxhYP40Rn1Z0Yh1yxQ2lMhdz2TOqIZu');
INSERT INTO `inst` (id, name, port, log_level, auth, auth_issuer) VALUES (1,'default',5000,'info','token','docker-auth-server');

INSERT INTO `account` (id,enabled,inst_id,username,password,hashpw,acl) VALUES (1,1,1,'admin','test','$2b$12$heObQDRTYvL8MR.xOPumWOWjrt2/P8YddPMTnZ202JxVQUPimW8D6','{"rules":[{"match":{"type":"repository","name":"*"},"actions":["*"],"comment":"管理员访问所有仓库"}]}');
INSERT INTO `account` (id,enabled,inst_id,username,password,hashpw,acl) VALUES (2,1,1,'test','test','$2b$12$heObQDRTYvL8MR.xOPumWOWjrt2/P8YddPMTnZ202JxVQUPimW8D6','{"rules":[{"match":{"type":"repository","name":"*"},"actions":["*"],"comment":"管理员访问所有仓库"}]}');
INSERT INTO `account` (id,enabled,inst_id,username,password,hashpw,acl) VALUES (3,1,1,'read','test','$2b$12$heObQDRTYvL8MR.xOPumWOWjrt2/P8YddPMTnZ202JxVQUPimW8D6','{"rules":[{"match":{"type":"repository","name":"*"},"actions":["pull"],"comment":"read只读"}]}');

-- 9. Views (SQLite does not support 'OR REPLACE', so drop first or just create)
DROP VIEW IF EXISTS artifact_manifest_view;
CREATE VIEW artifact_manifest_view AS
SELECT
    a.id AS id,
    a.inst_id,
    a.repo_id,
    a.repo_name,
    a.tag_name,
    a.full_name,
    a.created AS artifact_created,
    a.last_pushed AS artifact_last_pushed,
    a.last_pulled,
    m.id AS manifest_id,
    m.digest,
    m.media_type,
    m.size,
    m.os_arch,
    m.os,
    m.os_version,
    m.variant,
    m.features,
    m.parent_digest,
    m.created AS manifest_created,
    m.last_pushed AS manifest_last_pushed,
    m.annotations,
    m.config_digest,
    m.pushed_by,
    m.push_count
FROM artifact a LEFT JOIN manifest m ON a.manifest_id = m.id;

DROP VIEW IF EXISTS artifact_manifest_oci_view;
CREATE VIEW artifact_manifest_oci_view AS
SELECT
    a.id,
    a.tag_name,
    a.full_name,
    a.repo_id,
    a.repo_name,
    a.inst_id,
    m.id AS manifest_list_id,
    m.digest AS manifest_list_digest,
    m.media_type AS parent_media_type,
    m.size AS manifest_list_size,
    m.created AS manifest_list_created,
    child.id AS child_manifest_id,
    child.digest AS child_digest,
    child.os_arch,
    child.os,
    child.variant,
    child.size AS child_size,
    child.media_type AS child_media_type,
    child.os_version,
    child.features,
    child.created AS child_created,
    child.annotations,
    child.config_digest
FROM artifact a
         JOIN manifest m ON a.manifest_id = m.id
         JOIN manifest child ON m.digest = child.parent_digest
WHERE m.media_type IN (
                       'application/vnd.oci.image.index.v1+json','application/vnd.docker.distribution.manifest.list.v2+json'
    );