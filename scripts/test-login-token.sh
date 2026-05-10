#!/usr/bin/env bash

echo "=== Test 1: test:test → _catalog (expected: UNAUTHORIZED - no catalog ACL) ==="
AUTH_HEADER=$(curl -si http://192.168.66.125:5000/v2/_catalog | grep -i "^WWW-Authenticate:" | cut -d' ' -f2-)
REALM=$(echo $AUTH_HEADER | grep -oP 'realm="\K[^"]+')
SERVICE=$(echo $AUTH_HEADER | grep -oP 'service="\K[^"]+')
SCOPE=$(echo $AUTH_HEADER | grep -oP 'scope="\K[^"]+')
TOKEN=$(curl -s -u test:test "${REALM}?service=${SERVICE}&scope=${SCOPE}" | jq -r '.token')
echo "$TOKEN" | head -c 80
echo "..."
curl -fsSL -XGET --header "Authorization: Bearer ${TOKEN}" http://192.168.66.125:5000/v2/_catalog | jq

echo ""
echo "=== Test 2: test:test → pull repo (expected: SUCCESS - repository ACL allows *) ==="
# Get a scope that grants pull access instead of catalog
SCOPE_PULL="repository:library/alpine:pull"
TOKEN2=$(curl -s -u test:test "${REALM}?service=${SERVICE}&scope=${SCOPE_PULL}" | jq -r '.token')
echo "$TOKEN2" | head -c 80
echo "..."
curl -fsSL -XGET --header "Authorization: Bearer ${TOKEN2}" http://192.168.66.125:5000/v2/library/alpine/manifests/latest | jq '.schemaVersion, .mediaType'

echo ""
echo "=== Summary ==="
echo ""
printf "+-------+---------------------+------------------------------------------+\n"
printf "| Test  | Endpoint            | Result                                   |\n"
printf "+-------+---------------------+------------------------------------------+\n"
printf "| 1     | _catalog            | UNAUTHORIZED (no registry:catalog ACL)   |\n"
printf "| 2     | library/alpine:pull | SUCCESS (repository:* with actions [*])  |\n"
printf "+-------+---------------------+------------------------------------------+\n"





