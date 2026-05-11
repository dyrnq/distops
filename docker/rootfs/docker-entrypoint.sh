#!/usr/bin/env bash
set -eo pipefail
shopt -s nullglob




_main() {

if [ -n "$TZ" ]; then
    ln -snf "/usr/share/zoneinfo/$TZ" /etc/localtime || true
    (dpkg-reconfigure --frontend noninteractive tzdata >/dev/null 2>&1 || echo ${TZ} > /etc/timezone ) || true
fi

touch /etc/s6-overlay/s6-rc.d/user/contents.d/distops
touch /etc/s6-overlay/s6-rc.d/user/contents.d/redis
touch /etc/s6-overlay/s6-rc.d/user/contents.d/supervisor
exec /init

}

_main "$@"
