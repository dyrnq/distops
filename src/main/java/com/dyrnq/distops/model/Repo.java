package com.dyrnq.distops.model;

import java.io.Serializable;
import java.util.Date;
import java.time.LocalDateTime;
import org.noear.wood.annotation.*;
//import io.swagger.v3.oas.annotations.media.Schema;



import lombok.Data;

@Table("repo")
@Data
//repo
//@Schema(name = "Repo", description = "repo")
public class Repo implements Serializable {

private static final long serialVersionUID = 1L;
public static final String TABLE_NAME="repo";
@PrimaryKey
@com.fasterxml.jackson.databind.annotation.JsonSerialize(using = com.fasterxml.jackson.databind.ser.std.ToStringSerializer.class)
@Column("id")
//@Schema(description = "")
// 
// BIGINT
// 19
public Long id;


@com.fasterxml.jackson.databind.annotation.JsonSerialize(using = com.fasterxml.jackson.databind.ser.std.ToStringSerializer.class)
@Column("inst_id")
//@Schema(description = "")
// 
// BIGINT
// 19
public Long instId;


@Column("repo_name")
//@Schema(description = "")
// 
// VARCHAR
// 500
public String repoName;


@Column("artifact_count")
//@Schema(description = "")
// 
// INT
// 10
public Integer artifactCount;


@Column("last_pushed")
//@Schema(description = "最后推送时间")
// 最后推送时间
// DATETIME
// 26
public java.time.LocalDateTime lastPushed;


@Column("description")
//@Schema(description = "仓库描述")
// 仓库描述
// VARCHAR
// 1,000
public String description;




public static final String ID="id";
public static final String INST_ID="inst_id";
public static final String REPO_NAME="repo_name";
public static final String ARTIFACT_COUNT="artifact_count";
public static final String LAST_PUSHED="last_pushed";
public static final String DESCRIPTION="description";

/** GEN layui column
, {field: 'id', title: 'id'}
, {field: 'instId', title: 'instId'}
, {field: 'repoName', title: 'repoName'}
, {field: 'artifactCount', title: 'artifactCount'}
, {field: 'lastPushed', title: 'lastPushed'}
, {field: 'description', title: 'description'}
**/

//Customize BEGIN

//Customize END

}
