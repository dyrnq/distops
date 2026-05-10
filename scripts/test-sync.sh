#!/usr/bin/env bash
SCRIPT_DIR=$(cd "$(dirname "${BASH_SOURCE[0]}")" > /dev/null 2>&1 && pwd -P)

skopeo login      --tls-verify=false --username test --password test 192.168.66.125:5000


if [ -e "${SCRIPT_DIR}/.env" ]; then
  source "${SCRIPT_DIR}/.env"
fi

env |grep "PROXY"

skopeo --insecure-policy \
sync \
--keep-going --retry-times 50 --retry-delay 20s \
--src yaml \
--all \
--src-tls-verify=false \
--dest-tls-verify=false \
--dest-creds test:test \
--dest docker test-sync.yaml 192.168.66.125:5000/library

skopeo list-tags  --tls-verify=false docker://192.168.66.125:5000/library/bash


#ERRO[0050] Error copying ref "docker://bash:5.3.9"       error="copying image 11/16 from manifest list: determining manifest MIME type for docker://bash:5.3.9: reading manifest sha256:fd5cff6915064df602c186ca44d9d33720bbda4982f8c125a18e4cd723c2fac2 in docker.io/library/bash: toomanyrequests: You have reached your unauthenticated pull rate limit. https://www.docker.com/increase-rate-limit"
