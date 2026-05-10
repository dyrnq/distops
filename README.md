# distops

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


## ref

- [GitHub distribution](https://github.com/distribution/distribution)
- [GitHub skopeo](https://github.com/containers/skopeo)
- [GitHub regclient](https://github.com/regclient/regclient)
- [regclient.org](https://regclient.org)
- [CNCF distribution](https://distribution.github.io/distribution/)
- [GitHub docker_auth](https://github.com/cesanta/docker_auth)
- [Supervisor: A Process Control System](https://supervisord.org/)

