package com.dyrnq.distops.model;

import java.io.Serializable;
import java.util.Date;
import java.time.LocalDateTime;
import org.noear.wood.annotation.*;
//import io.swagger.v3.oas.annotations.media.Schema;



import lombok.Data;

@Table("global_config")
@Data
//global_config
//@Schema(name = "GlobalConfig", description = "global_config")
public class GlobalConfig implements Serializable {

private static final long serialVersionUID = 1L;
public static final String TABLE_NAME="global_config";
@PrimaryKey
@com.fasterxml.jackson.databind.annotation.JsonSerialize(using = com.fasterxml.jackson.databind.ser.std.ToStringSerializer.class)
@Column("id")
//@Schema(description = "")
// 
// BIGINT
// 19
public Long id;


@Column("name")
//@Schema(description = "")
// 
// VARCHAR
// 500
public String name;


@Column("value")
//@Schema(description = "")
// 
// VARCHAR
// 5,000
public String value;




public static final String ID="id";
public static final String NAME="name";
public static final String VALUE="value";

/** GEN layui column
, {field: 'id', title: 'id'}
, {field: 'name', title: 'name'}
, {field: 'value', title: 'value'}
**/

//Customize BEGIN

//Customize END

}
