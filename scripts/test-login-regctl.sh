#!/usr/bin/env bash


regctl registry set 192.168.66.125:5000 --tls disabled
echo "test" | regctl registry login 192.168.66.125:5000 -u test --pass-stdin

regctl manifest get 192.168.66.125:5000/library/nginx:1.28.2 --format raw-body | jq