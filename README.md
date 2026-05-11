# distops

<img src="https://a.dyrnq.com/distops/images/logo.png" alt="distribution" width="300" height="300"></img>

## description

Distops is a collection of tools for operations and maintenance surrounding the Docker registry(CNCF distribution).

## features

- Multi distribution management with UI
- Support both private registry Or proxy registry
- Embedded Auth server for Multi distribution(registry)
- Auth type include None, silly, htpasswd, and token
- Support RW, read-only account with ACL

Related projects

- [multi-registry-cache](https://github.com/dyrnq/mrc)

## build

```bash
git clone git@gitee.com:dyrnq/distops.git
cd distops
./mvnw clean package -Dmaven.test.skip=true -s ./settings.xml
```

## run with docker


```bash

mkdir /data/distops/persistent_data
chown 1000:1000 /data/distops/persistent_data
# 产生一个新的jwt secret 替换默认的 IDP32XTulsVIUZU+srFEUC9Lhu1wV+nd8iCJPoPA2zSFVAtWhCgpMEymxy5wFAZKMB9yROX31UjDzjwL66r1RA==
docker run -it --rm --entrypoint="" dyrnq/distops:latest bash -c "java -jar /distops.jar cli jwt"
docker \
run \
--detach \
--restart always \
--name distops \
--network host \
-e TZ=Asia/Shanghai \
-e SERVER_PORT="8080" \
-e SERVER_SESSION_TIMEOUT="172800" \
-e JWT_SECRET="__REPLACEME__" \
-e JAVA_OPTS="-server -Xms256m -Xms256m -Djava.awt.headless=true -Dfile.encoding=UTF-8 -Duser.timezone=Asia/Shanghai -Djava.net.preferIPv4Stack=true -Dspring.flyway.enabled=true" \
-v /data/distops/persistent_data:/data \
dyrnq/distops:latest

```

now, you can use browser open http://127.0.0.1:8080, default user password(admin/admin).

default database use sqlite.


Supports environment variables

| Variable Name                        | Meaning                                       | Default Value    |
|--------------------------------------|-----------------------------------------------|------------------|
| SERVER_PORT                          | Server port                                   | 12680            |
| PROJECT_HOME                         | Data directory                                | $HOME/apisixWeb  |
| SPRING_DATABASE_TYPE                 | Database type (h2, mysql, sqlite, postgresql) | sqlite           |
| SPRING_DATASOURCE_URL                | Database URL                                  |                  |
| SPRING_DATASOURCE_USERNAME           | Database username                             |                  |
| SPRING_DATASOURCE_PASSWORD           | Database password                             |                  |
| JWT_SECRET                           | jwt secret                                    |                  |
| SEVER_SESSION_TIMEOUT                | session timeout                               | 7200             |


## registry proxy

when using registry proxy e.g. `https://registry.k8s.io`, you may need config env `HTTPS_PROXY` and `NO_PROXY`.

e.g.

```bash
HTTPS_PROXY=http://192.168.66.1:7890
NO_PROXY=127.0.0.1,192.168.66.100
```


## screenshot

<img src="http://a.dyrnq.com/distops/images/Screenshot%202026-05-11%20at%2011-03-03%20distops.png" width="589" height="310" alt="distribution"></img>
<img src="http://a.dyrnq.com/distops/images/Screenshot%202026-05-11%20at%2011-03-32%20distops.png" width="589" height="310" alt="distribution"></img>
<img src="http://a.dyrnq.com/distops/images/Screenshot%202026-05-11%20at%2011-03-48%20distops.png" width="589" height="310" alt="distribution"></img>
<img src="http://a.dyrnq.com/distops/images/Screenshot%202026-05-11%20at%2011-04-03%20distops.png" width="589" height="310" alt="distribution"></img>
<img src="http://a.dyrnq.com/distops/images/Screenshot%202026-05-11%20at%2011-04-10%20distops.png" width="589" height="310" alt="distribution"></img>
<img src="http://a.dyrnq.com/distops/images/Screenshot%202026-05-11%20at%2011-04-23%20distops.png" width="589" height="310" alt="distribution"></img>
<img src="http://a.dyrnq.com/distops/images/Screenshot%202026-05-11%20at%2011-04-35%20distops.png" width="589" height="310" alt="distribution"></img>
<img src="http://a.dyrnq.com/distops/images/Screenshot%202026-05-11%20at%2011-04-41%20distops.png" width="589" height="310" alt="distribution"></img>
<img src="http://a.dyrnq.com/distops/images/Screenshot%202026-05-11%20at%2011-04-56%20distops.png" width="589" height="310" alt="distribution"></img>
<img src="http://a.dyrnq.com/distops/images/Screenshot%202026-05-11%20at%2011-05-17%20distops.png" width="589" height="310" alt="distribution"></img>
<img src="http://a.dyrnq.com/distops/images/Screenshot%202026-05-11%20at%2011-05-25%20distops.png" width="589" height="310" alt="distribution"></img>

## ref

- [GitHub distribution](https://github.com/distribution/distribution)
- [GitHub skopeo](https://github.com/containers/skopeo)
- [GitHub regclient](https://github.com/regclient/regclient)
- [regclient.org](https://regclient.org)
- [CNCF distribution](https://distribution.github.io/distribution/)
- [GitHub docker_auth](https://github.com/cesanta/docker_auth)
- [Supervisor: A Process Control System](https://supervisord.org/)

