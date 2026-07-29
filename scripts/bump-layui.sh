#!/usr/bin/env bash
# Bump the vendored layui distribution under src/main/resources/WEB-INF/static/lib/layui
# from a GitHub release zip.
#
# Usage:
#   bump-layui.sh [--ver <tag>] [--proxy <url>]
#
# Examples:
#   bump-layui.sh --ver v2.13.8
#   bump-layui.sh --ver v2.13.8 --proxy http://127.0.0.1:7890
SCRIPT_DIR=$(cd "$(dirname "${BASH_SOURCE[0]}")" > /dev/null 2>&1 && pwd -P)
CODE_BASE="${SCRIPT_DIR}/.."

ver=""
proxy=""

while [[ $# -gt 0 ]]; do
    case "$1" in
        --ver)
            ver="$2"
            shift 2
            ;;
        --proxy)
            proxy="$2"
            shift 2
            ;;
        -h|--help)
            sed -n '2,12p' "$0"
            exit 0
            ;;
        *)
            echo "Unknown argument: $1" >&2
            echo "Run '$0 --help' for usage." >&2
            exit 1
            ;;
    esac
done

# Default to the version currently vendored; override with --ver.
ver="${ver:-v2.13.5}"

curl_args=(-O -fSL -#)
if [[ -n "$proxy" ]]; then
    curl_args+=(--proxy "$proxy")
fi

echo "==> Downloading layui ${ver} from GitHub releases"
curl "${curl_args[@]}" https://github.com/layui/layui/releases/download/"${ver}"/layui-"${ver}".zip

echo "==> Extracting layui-${ver}.zip"
unzip -u -o layui-"${ver}".zip

echo "==> Inspecting extracted layout"
ls -l ./layui-${ver}/layui/

echo "==> Copying into ${CODE_BASE}/src/main/resources/WEB-INF/static/lib/layui/"
cp -v -f -R ./layui-${ver}/layui/.  ${CODE_BASE}/src/main/resources/WEB-INF/static/lib/layui/

echo "==> Cleaning up"
rm -rf layui-"${ver}".zip
rm -rf layui-${ver}

echo "==> Done. Vendored layui is now ${ver}."
