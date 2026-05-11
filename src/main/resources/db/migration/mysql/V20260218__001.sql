CREATE TABLE `user` (
  `id` VARCHAR(40) NOT NULL ,
  `name` VARCHAR(40) DEFAULT NULL ,
  `email` VARCHAR(256) DEFAULT NULL ,
  `phone` VARCHAR(256) DEFAULT NULL ,
  `pass` VARCHAR(512) DEFAULT NULL ,
  PRIMARY KEY (`id`)
);

CREATE TABLE `inst` (
  `id`                  BIGINT NOT NULL ,
  `name`                VARCHAR(256) DEFAULT NULL ,
  `port`                INT NOT NULL,
  `log_level`           VARCHAR(256) DEFAULT NULL,
  `os_arch`             VARCHAR(256) DEFAULT NULL ,
  `extra_yaml`          VARCHAR(5000) DEFAULT NULL ,
  `autostart`           INT DEFAULT 1,
  `autorestart`         INT DEFAULT 1,
  `enabled`             INT DEFAULT 1,
  `pid`                 BIGINT DEFAULT NULL,
  `auth`                VARCHAR(20) COMMENT '/silly/token/htpasswd/None',
  `auth_realm`          VARCHAR(512),
  `auth_service`        VARCHAR(512),
  `auth_issuer`         VARCHAR(512),
  `auth_private_key`    TEXT DEFAULT NULL COMMENT 'Private key for token signing (PEM format)',
  `auth_public_key`     TEXT DEFAULT NULL COMMENT 'Public key for token verification (PEM format)',
  `auth_jwks_json`      TEXT DEFAULT NULL COMMENT 'JWKS JSON content for token verification',
  `auth_key_type`       VARCHAR(20) DEFAULT 'EC' COMMENT 'Key type: EC or RSA',
  `auth_key_alg`        VARCHAR(20) DEFAULT 'ES256' COMMENT 'Key algorithm: ES256, ES384, ES512, RS256, RS384, RS512',
  `proxy_username`      VARCHAR(256) DEFAULT NULL,
  `proxy_password`      VARCHAR(256) DEFAULT NULL,
  `proxy_ttl`           VARCHAR(256) DEFAULT NULL,
  `proxy_remoteurl`     VARCHAR(512) DEFAULT NULL,
  `env`                 VARCHAR(512) DEFAULT NULL,
   PRIMARY KEY (`id`)
);

CREATE TABLE `repo` (
  `id` BIGINT NOT NULL ,
  `inst_id` BIGINT NOT NULL ,
  `repo_name` VARCHAR(500) NOT NULL ,
  `artifact_count` INT NOT NULL DEFAULT 0,
  `last_pushed` DATETIME(6) DEFAULT NULL COMMENT '最后推送时间',
  `description` VARCHAR(1000) DEFAULT NULL COMMENT '仓库描述',
  PRIMARY KEY (`id`)
);


CREATE TABLE `global_config` (
  `id` BIGINT NOT NULL ,
  `name` VARCHAR(500) NOT NULL ,
  `value` VARCHAR(5000) NOT NULL ,
  PRIMARY KEY (`id`)
);

INSERT INTO `user` VALUES ('1', 'admin','hello@admin.com','13988888888', '$2a$12$nXPoohJkpNbD1oSxtN0P1uGxhYP40Rn1Z0Yh1yxQ2lMhdz2TOqIZu');
INSERT INTO `inst` (id,name,port,log_level,auth,auth_issuer) VALUES (1,'default',5000,'info','token','docker-auth-server');


CREATE TABLE IF NOT EXISTS manifest (
id              BIGINT       NOT NULL PRIMARY KEY COMMENT '主键 ID',
inst_id         BIGINT       NOT NULL COMMENT '实例 ID',
repo_id         BIGINT       NOT NULL COMMENT '仓库 ID',
digest          VARCHAR(255) NOT NULL COMMENT 'manifest digest (sha256:xxx)',
media_type      VARCHAR(500) NOT NULL COMMENT 'MIME 类型',
`size`            BIGINT       DEFAULT NULL COMMENT '字节大小',
os_arch         VARCHAR(100) DEFAULT NULL COMMENT 'CPU 架构 (amd64, arm64, etc)',
os              VARCHAR(50)  DEFAULT NULL COMMENT '操作系统 (linux, windows)',
os_version      VARCHAR(50)  DEFAULT NULL COMMENT '操作系统版本',
variant         VARCHAR(50)  DEFAULT NULL COMMENT '架构变体 (v5, v6, v7, v8)',
features        VARCHAR(500) DEFAULT NULL COMMENT 'CPU 特性列表',
parent_digest   VARCHAR(255) DEFAULT NULL COMMENT '父 manifest digest',
created         DATETIME(6)  DEFAULT NULL COMMENT '镜像创建时间',
last_pushed     DATETIME(6)  DEFAULT NULL COMMENT '最后推送时间',
annotations     JSON         DEFAULT NULL COMMENT 'OCI annotations',
config_digest   VARCHAR(255) DEFAULT NULL COMMENT 'config layer digest',
pushed_by       VARCHAR(255) DEFAULT NULL COMMENT '推送者用户名',
push_count      INT          DEFAULT 0  COMMENT '被推送/引用次数',
UNIQUE KEY uk_manifest_digest (inst_id, digest),
INDEX idx_manifest_repo (repo_id),
INDEX idx_manifest_parent (parent_digest),
INDEX idx_manifest_platform (os_arch, os, variant),
INDEX idx_manifest_created (created)
) ENGINE=InnoDB COMMENT='OCI manifest 元数据表';


CREATE TABLE IF NOT EXISTS artifact (
id              BIGINT       NOT NULL PRIMARY KEY COMMENT '主键 ID',
inst_id         BIGINT       NOT NULL COMMENT '实例 ID',
repo_id         BIGINT       NOT NULL COMMENT '仓库 ID',
repo_name       VARCHAR(2000) DEFAULT NULL COMMENT '仓库名字,冗余字段',
manifest_id     BIGINT       NOT NULL COMMENT '关联的 manifest ID',
tag_name        VARCHAR(500) NOT NULL COMMENT 'tag 名称',
full_name       VARCHAR(2500) DEFAULT NULL COMMENT '完整名称 (repo:tag)',
created         DATETIME(6)  DEFAULT NULL COMMENT '创建时间',
last_pushed     DATETIME(6)  DEFAULT NULL COMMENT '最后推送时间',
last_pulled     DATETIME(6)  DEFAULT NULL COMMENT '最后pull时间',
UNIQUE KEY uk_artifact_repo_tag (inst_id, repo_id, tag_name),
INDEX idx_artifact_manifest (manifest_id),
INDEX idx_artifact_created (created)
) ENGINE=InnoDB COMMENT='artifact表';



CREATE OR REPLACE VIEW artifact_manifest_view AS
SELECT
    -- Artifact 表字段
    a.id AS id,
    a.inst_id,
    a.repo_id,
    a.repo_name,
    a.tag_name,
    a.full_name,
    a.created AS artifact_created,
    a.last_pushed AS artifact_last_pushed,
    a.last_pulled,

    -- Manifest 表字段
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

CREATE OR REPLACE VIEW artifact_manifest_oci_view AS
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
    -- 子 manifest 信息
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


CREATE TABLE IF NOT EXISTS account (
id BIGINT PRIMARY KEY COMMENT 'Primary key',
inst_id BIGINT NOT NULL COMMENT 'Instance ID',
username VARCHAR(500) NOT NULL COMMENT 'Username',
password VARCHAR(500) COMMENT 'Password (plain text, optional)',
hashpw VARCHAR(500) NOT NULL COMMENT 'BCrypt password hash',
acl JSON COMMENT 'ACL (Access Control List) configuration',
enabled INT NOT NULL DEFAULT 1 COMMENT 'Enable status: 1=enabled, 0=disabled',
created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT 'Creation timestamp',
updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Update timestamp',
UNIQUE KEY uk_inst_username (inst_id, username),
INDEX idx_inst_id (inst_id),
INDEX idx_username (username),
INDEX idx_enabled (enabled)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='User accounts for Docker Registry authentication';

INSERT INTO `account` (id,enabled,inst_id,username,password,hashpw,acl) VALUES (1,1,1,'admin','test','$2b$12$heObQDRTYvL8MR.xOPumWOWjrt2/P8YddPMTnZ202JxVQUPimW8D6','{"rules":[{"match":{"type":"repository","name":"*"},"actions":["*"],"comment":"管理员访问所有仓库"}]}');
INSERT INTO `account` (id,enabled,inst_id,username,password,hashpw,acl) VALUES (2,1,1,'test','test','$2b$12$heObQDRTYvL8MR.xOPumWOWjrt2/P8YddPMTnZ202JxVQUPimW8D6','{"rules":[{"match":{"type":"repository","name":"*"},"actions":["*"],"comment":"管理员访问所有仓库"}]}');
INSERT INTO `account` (id,enabled,inst_id,username,password,hashpw,acl) VALUES (3,1,1,'read','test','$2b$12$heObQDRTYvL8MR.xOPumWOWjrt2/P8YddPMTnZ202JxVQUPimW8D6','{"rules":[{"match":{"type":"repository","name":"*"},"actions":["pull"],"comment":"read只读"}]}');


insert into `inst` (id,name,port,log_level,proxy_remoteurl,proxy_ttl) VALUES (10000,'proxy-docker-io',                15000,'info','https://registry-1.docker.io',      '168h');
insert into `inst` (id,name,port,log_level,proxy_remoteurl,proxy_ttl) VALUES (10001,'proxy-registry-k8s-io',          15001,'info','https://registry.k8s.io',           '168h');
insert into `inst` (id,name,port,log_level,proxy_remoteurl,proxy_ttl) VALUES (10002,'proxy-k8s-gcr-io',               15002,'info','https://k8s.gcr.io',                '168h');
insert into `inst` (id,name,port,log_level,proxy_remoteurl,proxy_ttl) VALUES (10003,'proxy-gcr-io',                   15003,'info','https://gcr.io',                    '168h');
insert into `inst` (id,name,port,log_level,proxy_remoteurl,proxy_ttl) VALUES (10004,'proxy-ghcr-io',                  15004,'info','https://ghcr.io',                   '168h');
insert into `inst` (id,name,port,log_level,proxy_remoteurl,proxy_ttl) VALUES (10005,'proxy-quay-io',                  15005,'info','https://quay.io',                   '168h');
