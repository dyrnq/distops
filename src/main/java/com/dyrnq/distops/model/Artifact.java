package com.dyrnq.distops.model;

import java.io.Serializable;
import java.util.Date;
import java.time.LocalDateTime;
import org.noear.wood.annotation.*;
//import io.swagger.v3.oas.annotations.media.Schema;



import lombok.Data;

@Table("artifact")
@Data
//artifact表
//@Schema(name = "Artifact", description = "artifact表")
public class Artifact implements Serializable {

private static final long serialVersionUID = 1L;
public static final String TABLE_NAME="artifact";
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


@com.fasterxml.jackson.databind.annotation.JsonSerialize(using = com.fasterxml.jackson.databind.ser.std.ToStringSerializer.class)
@Column("manifest_id")
//@Schema(description = "关联的 manifest ID")
// 关联的 manifest ID
// BIGINT
// 19
public Long manifestId;


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


@Column("created")
//@Schema(description = "创建时间")
// 创建时间
// DATETIME
// 26
public java.time.LocalDateTime created;


@Column("last_pushed")
//@Schema(description = "最后推送时间")
// 最后推送时间
// DATETIME
// 26
public java.time.LocalDateTime lastPushed;


@Column("last_pulled")
//@Schema(description = "最后pull时间")
// 最后pull时间
// DATETIME
// 26
public java.time.LocalDateTime lastPulled;




public static final String ID="id";
public static final String INST_ID="inst_id";
public static final String REPO_ID="repo_id";
public static final String REPO_NAME="repo_name";
public static final String MANIFEST_ID="manifest_id";
public static final String TAG_NAME="tag_name";
public static final String FULL_NAME="full_name";
public static final String CREATED="created";
public static final String LAST_PUSHED="last_pushed";
public static final String LAST_PULLED="last_pulled";

/** GEN layui column
, {field: 'id', title: 'id'}
, {field: 'instId', title: 'instId'}
, {field: 'repoId', title: 'repoId'}
, {field: 'repoName', title: 'repoName'}
, {field: 'manifestId', title: 'manifestId'}
, {field: 'tagName', title: 'tagName'}
, {field: 'fullName', title: 'fullName'}
, {field: 'created', title: 'created'}
, {field: 'lastPushed', title: 'lastPushed'}
, {field: 'lastPulled', title: 'lastPulled'}
**/

//Customize BEGIN

//Customize END

}
