#!/usr/bin/env bash

docker logout

docker login 192.168.66.125:5000 --username=test --password=test


docker pull hello-world:latest
docker tag hello-world:latest 192.168.66.125:5000/hello-world:latest
docker push 192.168.66.125:5000/hello-world:latest

(
  docker pull debian:13.3
  docker tag debian:13.3 192.168.66.125:5000/test/hello/foo/debian:13.3
  docker push 192.168.66.125:5000/test/hello/foo/debian:13.3
  docker rmi debian:13.3
  docker rmi 192.168.66.125:5000/test/hello/foo/debian:13.3
  docker pull 192.168.66.125:5000/test/hello/foo/debian:13.3
)
(
  docker pull debian:13.2
  docker tag debian:13.2 192.168.66.125:5000/test/hello/foo/debian:13.2
  docker push 192.168.66.125:5000/test/hello/foo/debian:13.2
  docker rmi debian:13.2
  docker rmi 192.168.66.125:5000/test/hello/foo/debian:13.2
  docker pull 192.168.66.125:5000/test/hello/foo/debian:13.2
)


for i in "library/alpine:3" "library/nginx:1.28.2" "library/nginx:1.28.1" "library/busybox:1.37.0-musl" "library/busybox:1.37.0-glibc"; do

skopeo \
--insecure-policy \
copy \
--src-tls-verify=false \
--dest-tls-verify=false \
--all \
--retry-times 20 \
--dest-precompute-digests \
--dest-creds test:test \
docker://192.168.6.130:5000/"${i}" \
docker://192.168.66.125:5000/"${i}"

done


skopeo login      --tls-verify=false --username test --password test 192.168.66.125:5000
skopeo list-tags  --tls-verify=false docker://192.168.66.125:5000/library/busybox
skopeo inspect    --tls-verify=false docker://192.168.66.125:5000/library/busybox:1.37.0-musl
skopeo logout 192.168.66.125:5000
docker logout 192.168.66.125:5000
test=$(skopeo list-tags  --tls-verify=false docker://192.168.66.125:5000/library/busybox 2>&1)
grep -q "authentication required" <<< "${test}" && echo "assert pass"
