package com.dyrnq.distops.model;

import java.io.Serializable;
import java.util.Date;
import java.time.LocalDateTime;
import org.noear.wood.annotation.*;
//import io.swagger.v3.oas.annotations.media.Schema;



import lombok.Data;

@Table("inst")
@Data
//inst
//@Schema(name = "Inst", description = "inst")
public class Inst implements Serializable {

private static final long serialVersionUID = 1L;
public static final String TABLE_NAME="inst";
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
// 256
public String name;


@Column("port")
//@Schema(description = "")
// 
// INT
// 10
public Integer port;


@Column("log_level")
//@Schema(description = "")
// 
// VARCHAR
// 256
public String logLevel;


@Column("os_arch")
//@Schema(description = "")
// 
// VARCHAR
// 256
public String osArch;


@Column("extra_yaml")
//@Schema(description = "")
// 
// VARCHAR
// 5,000
public String extraYaml;


@Column("autostart")
//@Schema(description = "")
// 
// INT
// 10
public Integer autostart;


@Column("autorestart")
//@Schema(description = "")
// 
// INT
// 10
public Integer autorestart;


@Column("enabled")
//@Schema(description = "")
// 
// INT
// 10
public Integer enabled;


@com.fasterxml.jackson.databind.annotation.JsonSerialize(using = com.fasterxml.jackson.databind.ser.std.ToStringSerializer.class)
@Column("pid")
//@Schema(description = "")
// 
// BIGINT
// 19
public Long pid;


@Column("auth")
//@Schema(description = "/silly/token/htpasswd/None")
// /silly/token/htpasswd/None
// VARCHAR
// 20
public String auth;


@Column("auth_realm")
//@Schema(description = "")
// 
// VARCHAR
// 512
public String authRealm;


@Column("auth_service")
//@Schema(description = "")
// 
// VARCHAR
// 512
public String authService;


@Column("auth_issuer")
//@Schema(description = "")
// 
// VARCHAR
// 512
public String authIssuer;


@Column("auth_private_key")
//@Schema(description = "Private key for token signing (PEM format)")
// Private key for token signing (PEM format)
// TEXT
// 65,535
public String authPrivateKey;


@Column("auth_public_key")
//@Schema(description = "Public key for token verification (PEM format)")
// Public key for token verification (PEM format)
// TEXT
// 65,535
public String authPublicKey;


@Column("auth_jwks_json")
//@Schema(description = "JWKS JSON content for token verification")
// JWKS JSON content for token verification
// TEXT
// 65,535
public String authJwksJson;


@Column("auth_key_type")
//@Schema(description = "Key type: EC or RSA")
// Key type: EC or RSA
// VARCHAR
// 20
public String authKeyType;


@Column("auth_key_alg")
//@Schema(description = "Key algorithm: ES256, ES384, ES512, RS256, RS384, RS512")
// Key algorithm: ES256, ES384, ES512, RS256, RS384, RS512
// VARCHAR
// 20
public String authKeyAlg;


@Column("proxy_username")
//@Schema(description = "")
// 
// VARCHAR
// 256
public String proxyUsername;


@Column("proxy_password")
//@Schema(description = "")
// 
// VARCHAR
// 256
public String proxyPassword;


@Column("proxy_ttl")
//@Schema(description = "")
// 
// VARCHAR
// 256
public String proxyTtl;


@Column("proxy_remoteurl")
//@Schema(description = "")
// 
// VARCHAR
// 512
public String proxyRemoteurl;


@Column("env")
//@Schema(description = "")
// 
// VARCHAR
// 512
public String env;




public static final String ID="id";
public static final String NAME="name";
public static final String PORT="port";
public static final String LOG_LEVEL="log_level";
public static final String OS_ARCH="os_arch";
public static final String EXTRA_YAML="extra_yaml";
public static final String AUTOSTART="autostart";
public static final String AUTORESTART="autorestart";
public static final String ENABLED="enabled";
public static final String PID="pid";
public static final String AUTH="auth";
public static final String AUTH_REALM="auth_realm";
public static final String AUTH_SERVICE="auth_service";
public static final String AUTH_ISSUER="auth_issuer";
public static final String AUTH_PRIVATE_KEY="auth_private_key";
public static final String AUTH_PUBLIC_KEY="auth_public_key";
public static final String AUTH_JWKS_JSON="auth_jwks_json";
public static final String AUTH_KEY_TYPE="auth_key_type";
public static final String AUTH_KEY_ALG="auth_key_alg";
public static final String PROXY_USERNAME="proxy_username";
public static final String PROXY_PASSWORD="proxy_password";
public static final String PROXY_TTL="proxy_ttl";
public static final String PROXY_REMOTEURL="proxy_remoteurl";
public static final String ENV="env";

/** GEN layui column
, {field: 'id', title: 'id'}
, {field: 'name', title: 'name'}
, {field: 'port', title: 'port'}
, {field: 'logLevel', title: 'logLevel'}
, {field: 'osArch', title: 'osArch'}
, {field: 'extraYaml', title: 'extraYaml'}
, {field: 'autostart', title: 'autostart'}
, {field: 'autorestart', title: 'autorestart'}
, {field: 'enabled', title: 'enabled'}
, {field: 'pid', title: 'pid'}
, {field: 'auth', title: 'auth'}
, {field: 'authRealm', title: 'authRealm'}
, {field: 'authService', title: 'authService'}
, {field: 'authIssuer', title: 'authIssuer'}
, {field: 'authPrivateKey', title: 'authPrivateKey'}
, {field: 'authPublicKey', title: 'authPublicKey'}
, {field: 'authJwksJson', title: 'authJwksJson'}
, {field: 'authKeyType', title: 'authKeyType'}
, {field: 'authKeyAlg', title: 'authKeyAlg'}
, {field: 'proxyUsername', title: 'proxyUsername'}
, {field: 'proxyPassword', title: 'proxyPassword'}
, {field: 'proxyTtl', title: 'proxyTtl'}
, {field: 'proxyRemoteurl', title: 'proxyRemoteurl'}
, {field: 'env', title: 'env'}
**/

//Customize BEGIN

//Customize END

}
