#!/usr/bin/env bash



echo "htpasswd登录"
curl -u test:test http://192.168.66.125:5000/v2/_catalog
TOKEN=$(echo -n "test:test" | base64)
curl --header "Authorization: Basic ${TOKEN}" http://192.168.66.125:5000/v2/_catalog




#vagrant@server:~$ echo -n "test:test" | base64
#dGVzdDp0ZXN0
#vagrant@server:~$ base64 -w0 <<< "test:test"
#dGVzdDp0ZXN0Cg==




