package com.dyrnq.distops.model;

import java.io.Serializable;
import java.util.Date;
import java.time.LocalDateTime;
import org.noear.wood.annotation.*;
//import io.swagger.v3.oas.annotations.media.Schema;



import lombok.Data;

@Table("artifact_manifest_oci_view")
@Data
//VIEW
//@Schema(name = "ArtifactManifestOciView", description = "VIEW")
public class ArtifactManifestOciView implements Serializable {

private static final long serialVersionUID = 1L;
public static final String TABLE_NAME="artifact_manifest_oci_view";
@PrimaryKey
@com.fasterxml.jackson.databind.annotation.JsonSerialize(using = com.fasterxml.jackson.databind.ser.std.ToStringSerializer.class)
@Column("id")
//@Schema(description = "主键 ID")
// 主键 ID
// BIGINT
// 19
public Long id;


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


@com.fasterxml.jackson.databind.annotation.JsonSerialize(using = com.fasterxml.jackson.databind.ser.std.ToStringSerializer.class)
@Column("inst_id")
//@Schema(description = "实例 ID")
// 实例 ID
// BIGINT
// 19
public Long instId;


@com.fasterxml.jackson.databind.annotation.JsonSerialize(using = com.fasterxml.jackson.databind.ser.std.ToStringSerializer.class)
@Column("manifest_list_id")
//@Schema(description = "主键 ID")
// 主键 ID
// BIGINT
// 19
public Long manifestListId;


@Column("manifest_list_digest")
//@Schema(description = "manifest digest (sha256:xxx)")
// manifest digest (sha256:xxx)
// VARCHAR
// 255
public String manifestListDigest;


@Column("parent_media_type")
//@Schema(description = "MIME 类型")
// MIME 类型
// VARCHAR
// 500
public String parentMediaType;


@com.fasterxml.jackson.databind.annotation.JsonSerialize(using = com.fasterxml.jackson.databind.ser.std.ToStringSerializer.class)
@Column("manifest_list_size")
//@Schema(description = "字节大小")
// 字节大小
// BIGINT
// 19
public Long manifestListSize;


@Column("manifest_list_created")
//@Schema(description = "镜像创建时间")
// 镜像创建时间
// DATETIME
// 26
public java.time.LocalDateTime manifestListCreated;


@com.fasterxml.jackson.databind.annotation.JsonSerialize(using = com.fasterxml.jackson.databind.ser.std.ToStringSerializer.class)
@Column("child_manifest_id")
//@Schema(description = "主键 ID")
// 主键 ID
// BIGINT
// 19
public Long childManifestId;


@Column("child_digest")
//@Schema(description = "manifest digest (sha256:xxx)")
// manifest digest (sha256:xxx)
// VARCHAR
// 255
public String childDigest;


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


@Column("variant")
//@Schema(description = "架构变体 (v5, v6, v7, v8)")
// 架构变体 (v5, v6, v7, v8)
// VARCHAR
// 50
public String variant;


@com.fasterxml.jackson.databind.annotation.JsonSerialize(using = com.fasterxml.jackson.databind.ser.std.ToStringSerializer.class)
@Column("child_size")
//@Schema(description = "字节大小")
// 字节大小
// BIGINT
// 19
public Long childSize;


@Column("child_media_type")
//@Schema(description = "MIME 类型")
// MIME 类型
// VARCHAR
// 500
public String childMediaType;


@Column("os_version")
//@Schema(description = "操作系统版本")
// 操作系统版本
// VARCHAR
// 50
public String osVersion;


@Column("features")
//@Schema(description = "CPU 特性列表")
// CPU 特性列表
// VARCHAR
// 500
public String features;


@Column("child_created")
//@Schema(description = "镜像创建时间")
// 镜像创建时间
// DATETIME
// 26
public java.time.LocalDateTime childCreated;


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




public static final String ID="id";
public static final String TAG_NAME="tag_name";
public static final String FULL_NAME="full_name";
public static final String REPO_ID="repo_id";
public static final String REPO_NAME="repo_name";
public static final String INST_ID="inst_id";
public static final String MANIFEST_LIST_ID="manifest_list_id";
public static final String MANIFEST_LIST_DIGEST="manifest_list_digest";
public static final String PARENT_MEDIA_TYPE="parent_media_type";
public static final String MANIFEST_LIST_SIZE="manifest_list_size";
public static final String MANIFEST_LIST_CREATED="manifest_list_created";
public static final String CHILD_MANIFEST_ID="child_manifest_id";
public static final String CHILD_DIGEST="child_digest";
public static final String OS_ARCH="os_arch";
public static final String OS="os";
public static final String VARIANT="variant";
public static final String CHILD_SIZE="child_size";
public static final String CHILD_MEDIA_TYPE="child_media_type";
public static final String OS_VERSION="os_version";
public static final String FEATURES="features";
public static final String CHILD_CREATED="child_created";
public static final String ANNOTATIONS="annotations";
public static final String CONFIG_DIGEST="config_digest";

/** GEN layui column
, {field: 'id', title: 'id'}
, {field: 'tagName', title: 'tagName'}
, {field: 'fullName', title: 'fullName'}
, {field: 'repoId', title: 'repoId'}
, {field: 'repoName', title: 'repoName'}
, {field: 'instId', title: 'instId'}
, {field: 'manifestListId', title: 'manifestListId'}
, {field: 'manifestListDigest', title: 'manifestListDigest'}
, {field: 'parentMediaType', title: 'parentMediaType'}
, {field: 'manifestListSize', title: 'manifestListSize'}
, {field: 'manifestListCreated', title: 'manifestListCreated'}
, {field: 'childManifestId', title: 'childManifestId'}
, {field: 'childDigest', title: 'childDigest'}
, {field: 'osArch', title: 'osArch'}
, {field: 'os', title: 'os'}
, {field: 'variant', title: 'variant'}
, {field: 'childSize', title: 'childSize'}
, {field: 'childMediaType', title: 'childMediaType'}
, {field: 'osVersion', title: 'osVersion'}
, {field: 'features', title: 'features'}
, {field: 'childCreated', title: 'childCreated'}
, {field: 'annotations', title: 'annotations'}
, {field: 'configDigest', title: 'configDigest'}
**/

//Customize BEGIN

//Customize END

}
