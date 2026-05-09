package com.dyrnq.distops.model;

import java.io.Serializable;
import java.util.Date;
import java.time.LocalDateTime;
import org.noear.wood.annotation.*;
//import io.swagger.v3.oas.annotations.media.Schema;



import lombok.Data;

@Table("artifact_manifest_view")
@Data
//VIEW
//@Schema(name = "ArtifactManifestView", description = "VIEW")
public class ArtifactManifestView implements Serializable {

private static final long serialVersionUID = 1L;
public static final String TABLE_NAME="artifact_manifest_view";
@PrimaryKey
@com.fasterxml.jackson.databind.annotation.JsonSerialize(using = com.fasterxml.jackson.databind.ser.std.ToStringSerializer.class)
@Column("id")
//@Schema(description = "主键 ID")
// 主键 ID
// BIGINT
// 19
public Long id;


@com.fasterxml.jackson.databind.annotation.JsonSerialize(using = com.fasterxml.jackson.databind.ser.std.ToStringSerializer.class)
@Column("inst_id")
//@Schema(description = "实例 ID")
// 实例 ID
// BIGINT
// 19
public Long instId;


@com.fasterxml.jackson.databind.annotation.JsonSerialize(using = com.fasterxml.jackson.databind.ser.std.ToStringSerializer.class)
@Column("repo_id")
//@Schema(description = "仓库 ID")
// 仓库 ID
// BIGINT
// 19
public Long repoId;


@Column("repo_name")
//@Schema(description = "仓库名字,冗余字段")
// 仓库名字,冗余字段
// VARCHAR
// 2,000
public String repoName;


@Column("tag_name")
//@Schema(description = "tag 名称")
// tag 名称
// VARCHAR
// 500
public String tagName;


@Column("full_name")
//@Schema(description = "完整名称 (repo:tag)")
// 完整名称 (repo:tag)
// VARCHAR
// 2,500
public String fullName;


@Column("artifact_created")
//@Schema(description = "创建时间")
// 创建时间
// DATETIME
// 26
public java.time.LocalDateTime artifactCreated;


@Column("artifact_last_pushed")
//@Schema(description = "最后推送时间")
// 最后推送时间
// DATETIME
// 26
public java.time.LocalDateTime artifactLastPushed;


@Column("last_pulled")
//@Schema(description = "最后pull时间")
// 最后pull时间
// DATETIME
// 26
public java.time.LocalDateTime lastPulled;


@com.fasterxml.jackson.databind.annotation.JsonSerialize(using = com.fasterxml.jackson.databind.ser.std.ToStringSerializer.class)
@Column("manifest_id")
//@Schema(description = "主键 ID")
// 主键 ID
// BIGINT
// 19
public Long manifestId;


@Column("digest")
//@Schema(description = "manifest digest (sha256:xxx)")
// manifest digest (sha256:xxx)
// VARCHAR
// 255
public String digest;


@Column("media_type")
//@Schema(description = "MIME 类型")
// MIME 类型
// VARCHAR
// 500
public String mediaType;


@com.fasterxml.jackson.databind.annotation.JsonSerialize(using = com.fasterxml.jackson.databind.ser.std.ToStringSerializer.class)
@Column("size")
//@Schema(description = "字节大小")
// 字节大小
// BIGINT
// 19
public Long size;


@Column("os_arch")
//@Schema(description = "CPU 架构 (amd64, arm64, etc)")
// CPU 架构 (amd64, arm64, etc)
// VARCHAR
// 100
public String osArch;


@Column("os")
//@Schema(description = "操作系统 (linux, windows)")
// 操作系统 (linux, windows)
// VARCHAR
// 50
public String os;


@Column("os_version")
//@Schema(description = "操作系统版本")
// 操作系统版本
// VARCHAR
// 50
public String osVersion;


@Column("variant")
//@Schema(description = "架构变体 (v5, v6, v7, v8)")
// 架构变体 (v5, v6, v7, v8)
// VARCHAR
// 50
public String variant;


@Column("features")
//@Schema(description = "CPU 特性列表")
// CPU 特性列表
// VARCHAR
// 500
public String features;


@Column("parent_digest")
//@Schema(description = "父 manifest digest")
// 父 manifest digest
// VARCHAR
// 255
public String parentDigest;


@Column("manifest_created")
//@Schema(description = "镜像创建时间")
// 镜像创建时间
// DATETIME
// 26
public java.time.LocalDateTime manifestCreated;


@Column("manifest_last_pushed")
//@Schema(description = "最后推送时间")
// 最后推送时间
// DATETIME
// 26
public java.time.LocalDateTime manifestLastPushed;


@Column("annotations")
//@Schema(description = "OCI annotations")
// OCI annotations
// JSON
// 1,073,741,824
public String annotations;


@Column("config_digest")
//@Schema(description = "config layer digest")
// config layer digest
// VARCHAR
// 255
public String configDigest;


@Column("pushed_by")
//@Schema(description = "推送者用户名")
// 推送者用户名
// VARCHAR
// 255
public String pushedBy;


@Column("push_count")
//@Schema(description = "被推送/引用次数")
// 被推送/引用次数
// INT
// 10
public Integer pushCount;




public static final String ID="id";
public static final String INST_ID="inst_id";
public static final String REPO_ID="repo_id";
public static final String REPO_NAME="repo_name";
public static final String TAG_NAME="tag_name";
public static final String FULL_NAME="full_name";
public static final String ARTIFACT_CREATED="artifact_created";
public static final String ARTIFACT_LAST_PUSHED="artifact_last_pushed";
public static final String LAST_PULLED="last_pulled";
public static final String MANIFEST_ID="manifest_id";
public static final String DIGEST="digest";
public static final String MEDIA_TYPE="media_type";
public static final String SIZE="size";
public static final String OS_ARCH="os_arch";
public static final String OS="os";
public static final String OS_VERSION="os_version";
public static final String VARIANT="variant";
public static final String FEATURES="features";
public static final String PARENT_DIGEST="parent_digest";
public static final String MANIFEST_CREATED="manifest_created";
public static final String MANIFEST_LAST_PUSHED="manifest_last_pushed";
public static final String ANNOTATIONS="annotations";
public static final String CONFIG_DIGEST="config_digest";
public static final String PUSHED_BY="pushed_by";
public static final String PUSH_COUNT="push_count";

/** GEN layui column
, {field: 'id', title: 'id'}
, {field: 'instId', title: 'instId'}
, {field: 'repoId', title: 'repoId'}
, {field: 'repoName', title: 'repoName'}
, {field: 'tagName', title: 'tagName'}
, {field: 'fullName', title: 'fullName'}
, {field: 'artifactCreated', title: 'artifactCreated'}
, {field: 'artifactLastPushed', title: 'artifactLastPushed'}
, {field: 'lastPulled', title: 'lastPulled'}
, {field: 'manifestId', title: 'manifestId'}
, {field: 'digest', title: 'digest'}
, {field: 'mediaType', title: 'mediaType'}
, {field: 'size', title: 'size'}
, {field: 'osArch', title: 'osArch'}
, {field: 'os', title: 'os'}
, {field: 'osVersion', title: 'osVersion'}
, {field: 'variant', title: 'variant'}
, {field: 'features', title: 'features'}
, {field: 'parentDigest', title: 'parentDigest'}
, {field: 'manifestCreated', title: 'manifestCreated'}
, {field: 'manifestLastPushed', title: 'manifestLastPushed'}
, {field: 'annotations', title: 'annotations'}
, {field: 'configDigest', title: 'configDigest'}
, {field: 'pushedBy', title: 'pushedBy'}
, {field: 'pushCount', title: 'pushCount'}
**/

//Customize BEGIN

//Customize END

}
