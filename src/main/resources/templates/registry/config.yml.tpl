version: 0.1
log:
  accesslog:
    disabled: false
  level: ${inst.logLevel}
  formatter: text
  fields:
    service: registry
    environment: staging
storage:
  filesystem:
    rootdirectory: ${app_home}/registry/${inst.name}/data
    maxthreads: 100
  maintenance:
    uploadpurging:
      enabled: true
      age: 168h
      interval: 24h
      dryrun: false
    readonly:
      enabled: false
<#if inst.auth??>
auth:
<#if inst.auth?lower_case == "token"?lower_case>
  token:
    realm: http://192.168.66.125:${port}/auth
    autoredirectpath: /auth/token/
    autoredirect: false
    service: registry.docker.io
    issuer: docker-auth-server
    jwks: ${app_home}/registry/${inst.name}/config/jwks.json
    signingalgorithms: [EdDSA, HS256, HS384, HS512, RS256, RS384, RS512, ES256, ES384, ES512, PS256, PS384, PS512]
</#if>
<#if inst.auth?lower_case == "htpasswd"?lower_case>
  htpasswd:
    realm: basic-realm
    path: ${app_home}/registry/${inst.name}/config/htpasswd
</#if>
<#if inst.auth?lower_case == "silly"?lower_case>
  silly:
    realm: silly-realm
    service: silly-service
</#if>

</#if>
http:
  addr: :${inst.port?c}
  secret: asecretforlocaldevelopment
  headers:
    Access-Control-Expose-Headers: ['Docker-Content-Digest']
    Access-Control-Allow-Methods: ['HEAD', 'GET', 'OPTIONS', 'DELETE']
    Access-Control-Allow-Origin: ['*']
    X-Content-Type-Options: [nosniff]
  http2:
    disabled: false
  h2c:
    enabled: false
notifications:
  events:
    includereferences: true
  endpoints:
    - name: alistener
      disabled: false
      url: http://127.0.0.1:${port}/event/${inst.name}
      timeout: 1s
      threshold: 10
      backoff: 1s
