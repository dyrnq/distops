#!/usr/bin/env bash

AUTH_HEADER=$(curl -si http://192.168.66.125:5000/v2/_catalog | grep -i "^WWW-Authenticate:" | cut -d' ' -f2-)
REALM=$(echo $AUTH_HEADER | grep -oP 'realm="\K[^"]+')
SERVICE=$(echo $AUTH_HEADER | grep -oP 'service="\K[^"]+')
SCOPE=$(echo $AUTH_HEADER | grep -oP 'scope="\K[^"]+')
TOKEN=$(curl -s -u test:test "${REALM}?service=${SERVICE}&scope=${SCOPE}" | jq -r '.token')
echo $TOKEN;
curl -XGET --header "Authorization: Bearer ${TOKEN}" http://192.168.66.125:5000/v2/_catalog





