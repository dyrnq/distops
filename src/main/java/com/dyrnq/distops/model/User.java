package com.dyrnq.distops.model;

import java.io.Serializable;
import java.util.Date;
import java.time.LocalDateTime;
import org.noear.wood.annotation.*;
//import io.swagger.v3.oas.annotations.media.Schema;



import lombok.Data;

@Table("user")
@Data
//user
//@Schema(name = "User", description = "user")
public class User implements Serializable {

private static final long serialVersionUID = 1L;
public static final String TABLE_NAME="user";
@PrimaryKey
@Column("id")
//@Schema(description = "")
// 
// VARCHAR
// 40
public String id;


@Column("name")
//@Schema(description = "")
// 
// VARCHAR
// 40
public String name;


@Column("email")
//@Schema(description = "")
// 
// VARCHAR
// 256
public String email;


@Column("phone")
//@Schema(description = "")
// 
// VARCHAR
// 256
public String phone;


@Column("pass")
//@Schema(description = "")
// 
// VARCHAR
// 512
public String pass;




public static final String ID="id";
public static final String NAME="name";
public static final String EMAIL="email";
public static final String PHONE="phone";
public static final String PASS="pass";

/** GEN layui column
, {field: 'id', title: 'id'}
, {field: 'name', title: 'name'}
, {field: 'email', title: 'email'}
, {field: 'phone', title: 'phone'}
, {field: 'pass', title: 'pass'}
**/

//Customize BEGIN

//Customize END

}
