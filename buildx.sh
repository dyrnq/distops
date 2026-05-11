#!/usr/bin/env bash
set -Eeo pipefail

base_image="${base_image:-eclipse-temurin:25-jdk-noble}"
version="${version:-}";
push="${push:-false}"
repo="${repo:-dyrnq}"
image_name="${image_name:-distops}"
platforms="${platforms:-linux/amd64,linux/arm64/v8}"
curl_opts="${curl_opts:-}"
S6_OVERLAY_VERSION="3.2.3.0"
SKOPEO_VER="1.22.2"

while [ $# -gt 0 ]; do
    case "$1" in
        --base-image|--base)
            base_image="$2"
            shift
            ;;
        --version|--ver)
            version="$2"
            shift
            ;;
        --push)
            push="$2"
            shift
            ;;
        --curl-opts)
            curl_opts="$2"
            shift
            ;;
        --platforms)
            platforms="$2"
            shift
            ;;
        --repo)
            repo="$2"
            shift
            ;;
        --image-name|--image)
            image_name="$2"
            shift
            ;;
        --*)
            echo "Illegal option $1"
            ;;
    esac
    shift $(( $# > 0 ? 1 : 0 ))
done



# # https://docs.docker.com/build/building/multi-platform/
# docker run --privileged --rm tonistiigi/binfmt --install all
# # https://github.com/docker/buildx/issues/208
# docker run --privileged --rm multiarch/qemu-user---reset -p yes
# docker buildx create --name mbuilder --use 2>/dev/null || docker buildx use mbuilder
# docker buildx inspect --bootstrap



/bin/rm --verbose -rf ./docker/rootfs/distops*.jar
/bin/cp --verbose --force target/distops*.jar ./docker/rootfs/distops.jar


docker_file="./docker/Dockerfile"

## Compatible with local Docker builds
if [ -e ./docker/Dockerfile.dev ]; then
  ## just using for local dev ci
  docker_file="./docker/Dockerfile.dev"
fi
latest_tag="--tag $repo/$image_name:latest"

## Compatible with local Docker builds
if docker buildx version >/dev/null 2>&1; then
  docker buildx build \
  --platform ${platforms} \
  --output "type=image,push=$push" \
  --file "${docker_file}" ./docker \
  --build-arg SKOPEO_VER="${SKOPEO_VER}" \
  --build-arg S6_OVERLAY_VERSION="${S6_OVERLAY_VERSION}" \
  ${latest_tag}
else
  docker build \
  --file "${docker_file}" ./docker \
  --build-arg SKOPEO_VER="${SKOPEO_VER}" \
  --build-arg S6_OVERLAY_VERSION="${S6_OVERLAY_VERSION}" \
  ${latest_tag}
fi



