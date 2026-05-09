#!/usr/bin/env bash
SCRIPT_DIR=$(cd "$(dirname "${BASH_SOURCE[0]}")" > /dev/null 2>&1 && pwd -P)
CODE_BASE="${SCRIPT_DIR}/.."
ver="v2.13.5"
curl -O -fSL -# https://github.com/layui/layui/releases/download/"${ver}"/layui-"${ver}".zip
unzip -u -o layui-"${ver}".zip
ls -l ./layui-${ver}/layui/
cp -v -f -R ./layui-${ver}/layui/.  ${CODE_BASE}/src/main/resources/WEB-INF/static/lib/layui/
rm -rf layui-"${ver}".zip
rm -rf layui-"${ver}"

