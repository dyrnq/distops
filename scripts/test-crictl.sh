#!/usr/bin/env bash

containerd config default | tee /etc/containerd/config.toml
# https://github.com/containerd/containerd/blob/release/1.5/docs/hosts.md
# https://github.com/containerd/containerd/blob/release/2.1/docs/hosts.md
mkdir -p /etc/containerd/certs.d/192.168.66.125:5000
cat > /etc/containerd/certs.d/192.168.66.125:5000/hosts.toml<<EOF
server = "http://192.168.66.125:5000"
[host."http://192.168.66.125:5000"]
  capabilities = ["pull","resolve","push"]
  skip_verify = true
EOF
crictl pull --creds test:test 192.168.66.125:5000/library/nginx:1.28.2
#crictl pull 192.168.66.125:5000/library/nginx:1.28.2

ctr -n k8s.io image pull --skip-verify --plain-http --user test:test 192.168.66.125:5000/library/nginx:1.28.2
