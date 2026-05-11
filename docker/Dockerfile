FROM eclipse-temurin:25-jdk-noble

ARG DISTRIBUTION_VERSION
ARG user=dist
ARG group=dist
ARG uid=1000
ARG gid=1000
ARG GOSU_VERSION
ARG SKOPEO_VER
ARG REGCLIENT_VER
ARG S6_OVERLAY_VERSION


ENV \
DEBIAN_FRONTEND=noninteractive \
LANG=en_US.utf8 \
S6_OVERLAY_VERSION=${S6_OVERLAY_VERSION:-3.2.3.0} \
SKOPEO_VER=${SKOPEO_VER:-1.22.2} \
REGCLIENT_VER=${REGCLIENT_VER:-0.11.3} \
DISTRIBUTION_VERSION=${DISTRIBUTION_VERSION:-3.0.0} \
DIST_HOME=${DIST_HOME:-/data}


RUN \
set -eux; \
apt-get clean && \
apt-get update && \
apt-get -y upgrade && \
apt-get install -yq \
locales \
ca-certificates \
curl \
openssh-client \
psmisc \
procps \
iproute2 \
tree \
libfreetype6-dev \
fontconfig \
unzip \
less \
xz-utils \
gosu \
fonts-noto-cjk \
ttf-wqy-microhei \
wget \
jq \
supervisor \
apache2-utils \
nginx-full \
redis-server \
&& \
localedef -i en_US -c -f UTF-8 -A /usr/share/locale/locale.alias en_US.UTF-8 && \
( if id ubuntu 2>/dev/null; then  userdel -r ubuntu; fi ) && \
groupadd -g ${gid} ${group} && useradd -d "$DIST_HOME" -u ${uid} -g ${gid} -m -s /bin/bash ${user}; \
rm -rf /var/lib/apt/lists/*; \
ARCH="$(uname -m)"; \
case "${ARCH}" in \
   aarch64|arm64) \
     s6overlayArch="aarch64"; \
     distributionArch="arm64"; \
     ;; \
   armhf|armv7l) \
     s6overlayArch="armhf"; \
     distributionArch="armv7"; \
     c_rehash /etc/ssl/certs; \
     ;; \
   ppc64el|ppc64le) \
     s6overlayArch="ppc64le"; \
     distributionArch="ppc64le"; \
     ;; \
   amd64|x86_64) \
     s6overlayArch="x86_64"; \
     distributionArch="amd64"; \
     ;; \
   *) \
     echo "Unsupported arch: ${ARCH}"; \
     exit 1; \
     ;; \
esac; \
curl --retry 3 -f#SL https://github.com/just-containers/s6-overlay/releases/download/v${S6_OVERLAY_VERSION}/s6-overlay-${s6overlayArch}.tar.xz | tar -Jxpv -C /; \
curl --retry 3 -f#SL https://github.com/just-containers/s6-overlay/releases/download/v${S6_OVERLAY_VERSION}/s6-overlay-noarch.tar.xz | tar -Jxpv -C /; \
curl --retry 3 -f#SL https://github.com/just-containers/s6-overlay/releases/download/v${S6_OVERLAY_VERSION}/s6-overlay-symlinks-noarch.tar.xz | tar -Jxpv -C /; \
curl --retry 3 -f#SL https://github.com/distribution/distribution/releases/download/v${DISTRIBUTION_VERSION}/registry_${DISTRIBUTION_VERSION}_linux_${distributionArch}.tar.gz | tar -xvz -C /usr/local/bin/ registry; \
curl --retry 3 -f#SL https://ghfast.top/github.com/dyrnq/skopeo-binary/releases/download/v${SKOPEO_VER}/skopeo-linux-${distributionArch}.tar.xz | tar -xvJ -C /usr/local/bin; \
curl --retry 3 -f#SL https://ghfast.top/github.com/regclient/regclient/releases/download/v${REGCLIENT_VER}/regctl-linux-${distributionArch} -o /usr/local/bin/regctl; \
chmod +x /usr/local/bin/registry; \
chmod +x /usr/local/bin/regctl; \
chmod +x /usr/local/bin/skopeo; \
registry --version


VOLUME "${DIST_HOME}"



COPY rootfs /
EXPOSE 8080

STOPSIGNAL SIGTERM

ENTRYPOINT ["/docker-entrypoint.sh"]
