#!/usr/bin/env bash

username="read"
password="test"
(
docker logout
docker login 192.168.66.125:5000 --username=${username} --password=${password}
)
(
  docker pull debian:13.2
  docker tag debian:13.2 192.168.66.125:5000/test/hello/foo/debian:13.2
  
  if docker push 192.168.66.125:5000/test/hello/foo/debian:13.2; then
    :
  else
    echo "read only user ${username}, test passed. "
  fi

)
