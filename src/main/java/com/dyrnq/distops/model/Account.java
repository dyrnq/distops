package com.dyrnq.distops.model;

import java.io.Serializable;
import java.util.Date;
import java.time.LocalDateTime;
import org.noear.wood.annotation.*;
//import io.swagger.v3.oas.annotations.media.Schema;



import lombok.Data;

@Table("account")
@Data
//User accounts for Docker Registry authentication
//@Schema(name = "Account", description = "User accounts for Docker Registry authentication")
public class Account implements Serializable {

private static final long serialVersionUID = 1L;
public static final String TABLE_NAME="account";
@PrimaryKey
@com.fasterxml.jackson.databind.annotation.JsonSerialize(using = com.fasterxml.jackson.databind.ser.std.ToStringSerializer.class)
@Column("id")
//@Schema(description = "Primary key")
// Primary key
// BIGINT
// 19
public Long id;


@com.fasterxml.jackson.databind.annotation.JsonSerialize(using = com.fasterxml.jackson.databind.ser.std.ToStringSerializer.class)
@Column("inst_id")
//@Schema(description = "Instance ID")
// Instance ID
// BIGINT
// 19
public Long instId;


@Column("username")
//@Schema(description = "Username")
// Username
// VARCHAR
// 500
public String username;


@Column("password")
//@Schema(description = "Password (plain text, optional)")
// Password (plain text, optional)
// VARCHAR
// 500
public String password;


@Column("hashpw")
//@Schema(description = "BCrypt password hash")
// BCrypt password hash
// VARCHAR
// 500
public String hashpw;


@Column("acl")
//@Schema(description = "ACL (Access Control List) configuration")
// ACL (Access Control List) configuration
// JSON
// 1,073,741,824
public String acl;


@Column("enabled")
//@Schema(description = "Enable status: 1=enabled, 0=disabled")
// Enable status: 1=enabled, 0=disabled
// INT
// 10
public Integer enabled;


@Column("created_at")
//@Schema(description = "Creation timestamp")
// Creation timestamp
// DATETIME
// 19
public java.time.LocalDateTime createdAt;


@Column("updated_at")
//@Schema(description = "Update timestamp")
// Update timestamp
// DATETIME
// 19
public java.time.LocalDateTime updatedAt;




public static final String ID="id";
public static final String INST_ID="inst_id";
public static final String USERNAME="username";
public static final String PASSWORD="password";
public static final String HASHPW="hashpw";
public static final String ACL="acl";
public static final String ENABLED="enabled";
public static final String CREATED_AT="created_at";
public static final String UPDATED_AT="updated_at";

/** GEN layui column
, {field: 'id', title: 'id'}
, {field: 'instId', title: 'instId'}
, {field: 'username', title: 'username'}
, {field: 'password', title: 'password'}
, {field: 'hashpw', title: 'hashpw'}
, {field: 'acl', title: 'acl'}
, {field: 'enabled', title: 'enabled'}
, {field: 'createdAt', title: 'createdAt'}
, {field: 'updatedAt', title: 'updatedAt'}
**/

//Customize BEGIN
@Column("inst_name")

public String instName;
//Customize END

}
