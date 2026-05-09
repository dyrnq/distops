#!/usr/bin/env bash
# shellcheck disable=SC2086


iface="${iface:-enp0s8}"

wait4x_image="${wait4x_image:-atkrad/wait4x:2.14}"
mysql5_image="${mysql5_image:-mysql:5.7.44}"
mysql8_image="${mysql8_image:-mysql:8.4.8}"
pg_image="${pg_image:-postgres:12.14}"
adminer_image="${adminer_image:-adminer:5.4.2}"
minio_image="${minio_image:-minio/minio:RELEASE.2025-02-28T09-55-16Z}"
nginx_image="${nginx_image:-nginx:1.28.2}"
distribution_image="${distribution_image:-distribution/distribution:3.0}"




while [ $# -gt 0 ]; do
    case "$1" in
        --iface|-i)
            iface="$2"
            shift
            ;;
        --*)
            echo "Illegal option $1"
            ;;
    esac
    shift $(( $# > 0 ? 1 : 0 ))
done

ip4=$(/sbin/ip -o -4 addr list "${iface}" | awk '{print $4}' |cut -d/ -f1 | head -n1);


command_exists() {
    command -v "$@" > /dev/null 2>&1
}



fun_add_mynet(){

docker network inspect mynet &>/dev/null || docker network create --subnet 172.18.0.0/16 --gateway 172.18.0.1 --driver bridge mynet

}



fun_install_misc(){
docker rm -f mysql57 2>/dev/null || true
docker rm -f mysql8 2>/dev/null || true
docker rm -f postgres12 2>/dev/null || true
docker rm -f adminer 2>/dev/null || true
docker rm -f minio 2>/dev/null || true

mkdir -p $HOME/var/lib/mysql
docker run -d --name mysql57 \
--restart always \
--network mynet \
-e MYSQL_ROOT_PASSWORD=666666 \
-v $HOME/var/lib/mysql:/var/lib/mysql \
-p 3306:3306 \
${mysql5_image} --character-set-server=utf8mb4 --collation-server=utf8mb4_unicode_ci --default-time-zone=+8:00

mkdir -p $HOME/var/lib/mysql8
docker run -d --name mysql8 \
--restart always \
--network mynet \
-e MYSQL_ROOT_PASSWORD=666666 \
-v $HOME/var/lib/mysql8:/var/lib/mysql \
-p 13306:3306 \
${mysql8_image} --character-set-server=utf8mb4 --collation-server=utf8mb4_unicode_ci --default-time-zone=+8:00 --innodb-dedicated-server=on


mkdir -p $HOME/var/lib/postgresql/data

docker run -d --name postgres12 \
--restart always \
--network mynet \
-e POSTGRES_PASSWORD=666666 \
-p 5432:5432 \
-v $HOME/var/lib/postgresql/data:/var/lib/postgresql/data \
${pg_image}


docker run -d --name=adminer --restart always --network mynet -p 18080:8080 ${adminer_image}



}

fun_initdb(){
WAIT4X_CMD="docker run --net host --rm --name='wait4x' ${wait4x_image}"  
WAIT4X_CMD="wait4x"

${WAIT4X_CMD} mysql root:666666@tcp\(127.0.0.1:3306\)/mysql --interval 1s --timeout 360s && \
mysql --host=127.0.0.1 --port=3306 --user=root --password=666666 --loose-default-character-set=utf8 -e "CREATE DATABASE if not exists distops DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;show databases;"


${WAIT4X_CMD} mysql root:666666@tcp\(127.0.0.1:13306\)/mysql --interval 1s --timeout 360s && \
mysql --host=127.0.0.1 --port=13306 --user=root --password=666666 -e "CREATE DATABASE if not exists distops DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;show databases;"


${WAIT4X_CMD} postgresql 'postgres://postgres:666666@127.0.0.1:5432/postgres?sslmode=disable' --interval 1s --timeout 360s && \
docker exec -i postgres12 bash <<'EOF'
if [ $(psql -tA --username "postgres" -c "select count(1) from pg_database where datname='distops'") = "0" ]; then
    psql -v ON_ERROR_STOP=1 --username "postgres" --no-password -c "create database distops with encoding='utf8' TEMPLATE template0;"
fi
psql -v ON_ERROR_STOP=1 --username "postgres" --no-password -c "\l";
EOF
}


fun_install_minio(){
docker rm -f minio 2>/dev/null || true
mkdir -p $HOME/minio/data
docker run -d \
--name=minio \
--restart always \
--network host \
-e "MINIO_ROOT_USER=minioadmin" \
-e "MINIO_ROOT_PASSWORD=minioadmin" \
-v $HOME/minio/data:/data \
${minio_image} server /data --address ":19000" --console-address ":19001"



sleep 5s;

docker run -i --rm --network host --entrypoint '' minio/mc bash<<EOF
mc alias set myminio http://localhost:19000 minioadmin minioadmin;
mc admin user svcacct add --access-key "vUR3oLMF5ds8gWCP" --secret-key "odWFIZukYrw9dY0G5ezDKMZWbhU0S4oD" myminio minioadmin 2>/dev/null || true;
EOF

docker run -i --rm --network host --entrypoint '' minio/mc bash<<EOF
mc alias set myminio http://localhost:19000 minioadmin minioadmin;
mc mb myminio/my-bucket
mc anonymous set download myminio/my-bucket
EOF

}

fun_nginx_static(){

port=9980
name=nginx-$port
docker rm -f $name 2>/dev/null 1>/dev/null || true
cat >$HOME/nginx-$port.conf<<EOF
server {
    listen 80 reuseport backlog=8192 so_keepalive=10m:30s:10;
    keepalive_requests 30000;
    keepalive_time 30m;
    keepalive_timeout 150s;


    location / {
        root /usr/share/nginx/html;
        index index.html index.htm;
        autoindex on;
        autoindex_exact_size on;
        autoindex_localtime on;
        types {
            text/plain yaml;
            text/plain md;
            text/plain yml;
            text/plain conf;
            text/plain properties;
            text/plain service;
            text/plain h;
            text/plain c;
            text/plain sed;
            text/plain sh;
            text/plain xml;
            text/html html;
        }
    }
}
EOF

docker \
run -d \
--name $name \
--restart always \
--network mynet \
--add-host=host.docker.internal:host-gateway \
-p $port:80 \
-v $HOME/nginx-$port.conf:/etc/nginx/conf.d/default.conf \
${nginx_image}
}

fun_nginx_proxy_minio(){
port=9982
name=nginx-$port
docker rm -f $name 2>/dev/null 1>/dev/null || true
cat >$HOME/nginx-$port.conf<<EOF
upstream backend {
   server host.docker.internal:19000;
   keepalive 32;
   keepalive_requests 1000;
   keepalive_time 1h;
   keepalive_timeout 120s;
}

server {
    listen 80 reuseport backlog=8192 so_keepalive=10m:30s:10;
    keepalive_requests 30000;
    keepalive_time 30m;
    keepalive_timeout 150s;
    location / {
      rewrite ^(/.*)\$ /my-bucket\$1 break;
      proxy_pass http://backend;
      proxy_http_version 1.1;
      proxy_set_header Connection "";
      proxy_set_header Host \$host;
      proxy_set_header X-Real-IP \$remote_addr;
      proxy_set_header X-Forwarded-For \$proxy_add_x_forwarded_for;
      proxy_set_header X-Forwarded-Proto \$scheme;
      proxy_set_header X-Forwarded-Port  \$server_port;
      proxy_set_header X-Forwarded-Host  \$host;
      proxy_hide_header X-Protocol-Hide;
      proxy_pass_header Server;
      proxy_busy_buffers_size 512k;
      proxy_buffers 8 512k;
      proxy_buffer_size 256k;
      proxy_read_timeout 1800;
      proxy_connect_timeout 1800;
      proxy_send_timeout 1800;
      client_max_body_size 50M;
      proxy_next_upstream error timeout invalid_header http_500 http_502 http_503 http_504;
      proxy_ssl_protocols TLSv1 TLSv1.1 TLSv1.2 TLSv1.3;
      proxy_ssl_ciphers HIGH:!aNULL:!MD5;
      proxy_ssl_verify off;
      proxy_set_header cookie \$http_cookie;
      chunked_transfer_encoding off;
      port_in_redirect off;
      absolute_redirect off;
      proxy_socket_keepalive on;
    }
}
EOF

docker \
run -d \
--name $name \
--restart always \
--network mynet \
--add-host=host.docker.internal:host-gateway \
-p $port:80 \
-v $HOME/nginx-$port.conf:/etc/nginx/conf.d/default.conf \
${nginx_image}
}


fun_distribution(){
mkdir -p $HOME/var/lib/distribution/data
mkdir -p $HOME/var/lib/distribution/config

cat >$HOME/var/lib/distribution/config/config.yml<<EOF
version: 0.1
log:
  accesslog:
    disabled: true
  level: debug
  formatter: text
  fields:
    service: registry
    environment: staging
#   hooks:
#     - type: mail
#       disabled: true
#       levels:
#         - panic
#       options:
#         smtp:
#           addr: mail.example.com:25
#           username: mailuser
#           password: password
#           insecure: true
#         from: sender@example.com
#         to:
#           - errors@example.com
# loglevel: debug # deprecated: use "log"
storage:
  filesystem:
    rootdirectory: /var/lib/registry
    maxthreads: 100
#   azure:
#     accountname: accountname
#     accountkey: base64encodedaccountkey
#     container: containername
#     rootdirectory: /az/object/name/prefix
#     credentials:
#       type: client_secret
#       clientid: client_id_string
#       tenantid: tenant_id_string
#       secret: secret_string
#     max_retries: 10
#     retry_delay: 100ms
#   gcs:
#     bucket: bucketname
#     keyfile: /path/to/keyfile
#     credentials:
#       type: service_account
#       project_id: project_id_string
#       private_key_id: private_key_id_string
#       private_key: private_key_string
#       client_email: client@example.com
#       client_id: client_id_string
#       auth_uri: http://example.com/auth_uri
#       token_uri: http://example.com/token_uri
#       auth_provider_x509_cert_url: http://example.com/provider_cert_url
#       client_x509_cert_url: http://example.com/client_cert_url
#     rootdirectory: /gcs/object/name/prefix
#     chunksize: 5242880
#   s3:
#     accesskey: awsaccesskey
#     secretkey: awssecretkey
#     region: us-west-1
#     regionendpoint: http://myobjects.local
#     forcepathstyle: true
#     accelerate: false
#     bucket: bucketname
#     encrypt: true
#     keyid: mykeyid
#     secure: true
#     v4auth: true
#     chunksize: 5242880
#     multipartcopychunksize: 33554432
#     multipartcopymaxconcurrency: 100
#     multipartcopythresholdsize: 33554432
#     rootdirectory: /s3/object/name/prefix
#     usedualstack: false
#     usefipsendpoint: false
#     loglevel: debug
#   inmemory:  # This driver takes no parameters
#   tag:
#     concurrencylimit: 8
#   delete:
#     enabled: false
#   redirect:
#     disable: false
#   cache:
#     blobdescriptor: redis
#     blobdescriptorsize: 10000
  maintenance:
    uploadpurging:
      enabled: true
      age: 168h
      interval: 24h
      dryrun: false
    readonly:
      enabled: false
auth:
#   silly:
#     realm: silly-realm
#     service: silly-service
#   token:
#     autoredirect: true
#     realm: token-realm
#     service: token-service
#     issuer: registry-token-issuer
#     rootcertbundle: /root/certs/bundle
#     jwks: /path/to/jwks
#     signingalgorithms:
#         - EdDSA
#         - HS256
  htpasswd:
    realm: basic-realm
    path: /etc/distribution/htpasswd
# middleware:
#   registry:
#     - name: ARegistryMiddleware
#       options:
#         foo: bar
#   repository:
#     - name: ARepositoryMiddleware
#       options:
#         foo: bar
#   storage:
#     - name: cloudfront
#       options:
#         baseurl: https://my.cloudfronted.domain.com/
#         privatekey: /path/to/pem
#         keypairid: cloudfrontkeypairid
#         duration: 3000s
#         ipfilteredby: awsregion
#         awsregion: us-east-1, use-east-2
#         updatefrequency: 12h
#         iprangesurl: https://ip-ranges.amazonaws.com/ip-ranges.json
#   storage:
#     - name: redirect
#       options:
#         baseurl: https://example.com/
http:
  addr: 0.0.0.0:5000
#   prefix: /my/nested/registry/
#   host: https://myregistryaddress.org:5000
#   secret: asecretforlocaldevelopment
#   relativeurls: false
#   draintimeout: 60s
#   tls:
#     certificate: /path/to/x509/public
#     key: /path/to/x509/private
#     clientcas:
#       - /path/to/ca.pem
#       - /path/to/another/ca.pem
#     clientauth: require-and-verify-client-cert
#     letsencrypt:
#       cachefile: /path/to/cache-file
#       email: emailused@letsencrypt.com
#       hosts: [myregistryaddress.org]
#       directoryurl: https://acme-v02.api.letsencrypt.org/directory
#   debug:
#     addr: localhost:5001
#     prometheus:
#       enabled: true
#       path: /metrics
  headers:
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
      url: http://192.168.66.1:12680/event
    #   headers: <http.Header>
      timeout: 1s
      threshold: 10
      backoff: 1s
    #   ignoredmediatypes:
    #     - application/octet-stream
    #   ignore:
    #     mediatypes:
    #        - application/octet-stream
    #     actions:
    #        - pull
# redis:
#   tls:
#     certificate: /path/to/cert.crt
#     key: /path/to/key.pem
#     rootcas:
#       - /path/to/ca.pem
#   addrs: [localhost:6379]
#   password: asecret
#   db: 0
#   dialtimeout: 10ms
#   readtimeout: 10ms
#   writetimeout: 10ms
#   maxidleconns: 16
#   poolsize: 64
#   connmaxidletime: 300s
#   tls:
#     enabled: false
# health:
#   storagedriver:
#     enabled: true
#     interval: 10s
#     threshold: 3
#   file:
#     - file: /path/to/checked/file
#       interval: 10s
#   http:
#     - uri: http://server.to.check/must/return/200
#       headers:
#         Authorization: [Basic QWxhZGRpbjpvcGVuIHNlc2FtZQ==]
#       statuscode: 200
#       timeout: 3s
#       interval: 10s
#       threshold: 3
#   tcp:
#     - addr: redis-server.domain.com:6379
#       timeout: 3s
#       interval: 10s
#       threshold: 3
# proxy:
#   remoteurl: https://registry-1.docker.io
#   username: [username]
#   password: [password]
#   exec:
#     command: docker-credential-helper
#     lifetime: 1h
#   ttl: 168h
# validation:
#   manifests:
#     urls:
#       allow:
#         - ^https?://([^/]+\.)*example\.com/
#       deny:
#         - ^https?://www\.example\.com/
#     indexes:
#       platforms: List
#       platformlist:
#       - architecture: amd64
#         os: linux
EOF

docker run -it --rm --entrypoint "" httpd:2 htpasswd -Bbn test test > $HOME/var/lib/distribution/config/htpasswd
cat <$HOME/var/lib/distribution/config/htpasswd

docker rm -f d1 >/dev/null 2>&1
docker run -d \
-p 5000:5000 \
--restart always \
--name d1 \
-e OTEL_TRACES_EXPORTER=none \
-v $HOME/var/lib/distribution/data:/var/lib/registry \
-v $HOME/var/lib/distribution/config/htpasswd:/etc/distribution/htpasswd \
-v $HOME/var/lib/distribution/config/config.yml:/etc/distribution/config.yml \
${distribution_image}

}


fun_add_mynet
fun_install_misc
fun_initdb
fun_install_minio
# fun_nginx_static
# fun_nginx_proxy_minio
# fun_distribution

docker stop minio mysql57 postgres12
