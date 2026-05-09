#!/usr/bin/env bash
mirrors="http://192.168.6.130:5000"
insecure_registries="192.168.66.125:5000,192.168.6.130:5000"
# echo "${mirrors}"
while [ $# -gt 0 ]; do
    case "$1" in
        --mirrors|-m|-M)
            mirrors="$2"
            shift
            ;;
        --insecure_registries|-ir|-I)
            insecure_registries="$2"
            shift
            ;;
        --*)
            echo "Illegal option $1"
            ;;
    esac
    shift $(( $# > 0 ? 1 : 0 ))
done


IFS=',' read -r -a mirrors_arr <<< ${mirrors}
IFS=',' read -r -a insecure_registries_arr <<< ${insecure_registries}

echo "check jq whether installed..."
if command -v jq >/dev/null 2>&1; then
    echo "check: OK; /etc/docker/daemon.json is writable."
else
    echo "check: failed; jq not installed. please install it."
    exit 1
fi

echo "check /etc/docker/daemon.json whether exists and writable..."

if [ ! -w "/etc/docker/daemon.json" ]; then
    echo "check: failed; /etc/docker/daemon.json not writable, please check it."
    exit 1
else
    echo "check: OK; /etc/docker/daemon.json is writable."
fi

#########################################################
echo "current mirrors:"
jq -r ".\"registry-mirrors\"" /etc/docker/daemon.json

mirrors_json=""
for element in "${mirrors_arr[@]}" ;do
    mirrors_json=${mirrors_json}"\""${element}\"","
done
mirrors_json="[${mirrors_json%?}]"

echo "new mirrors:"

echo "${mirrors_json}"
echo ""
echo "update mirrors..."

jq ".\"registry-mirrors\" = ${mirrors_json}" /etc/docker/daemon.json > /etc/docker/daemon.json.tmp && mv /etc/docker/daemon.json.tmp /etc/docker/daemon.json

#########################################################
echo "current insecure-registries:"
jq -r ".\"insecure-registries\"" /etc/docker/daemon.json

insecure_registries_json=""
for element in "${insecure_registries_arr[@]}" ;do
    insecure_registries_json=${insecure_registries_json}"\""${element}\"","
done
insecure_registries_json="[${insecure_registries_json%?}]"

echo "new insecure-registries:"

echo "${insecure_registries_json}"
echo ""
echo "update insecure-registries..."

jq ".\"insecure-registries\" = ${insecure_registries_json}" /etc/docker/daemon.json > /etc/docker/daemon.json.tmp && mv /etc/docker/daemon.json.tmp /etc/docker/daemon.json

############################################################

echo "current daemon.json:"
cat </etc/docker/daemon.json

echo ""
echo "restart docker..."

systemctl daemon-reload
systemctl restart docker
