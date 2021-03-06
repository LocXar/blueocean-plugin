#!/usr/bin/env bash
set -eu -o pipefail

HERE="$(cd -P "$( dirname "${BASH_SOURCE[0]}" )" && pwd)"

setup_nice_output() {
  # check if stdout is a terminal...
  if [ -t 1 ]; then

    # see if it supports colors...
    ncolors=$(tput colors)

    if test -n "$ncolors" && test $ncolors -ge 8; then
        bold="$(tput bold)"
        underline="$(tput smul)"
        standout="$(tput smso)"
        normal="$(tput sgr0)"
        black="$(tput setaf 0)"
        red="$(tput setaf 1)"
        green="$(tput setaf 2)"
        yellow="$(tput setaf 3)"
        blue="$(tput setaf 4)"
        magenta="$(tput setaf 5)"
        cyan="$(tput setaf 6)"
        white="$(tput setaf 7)"
    fi
  fi
}

new_build_container() {
  local build_image=$1; shift

  build_container=$(docker create -i -v "$HERE":/build -w /build "$build_image" /bin/cat)
  echo "$build_container" > "$HERE/.build_container"
}

delete_build_container() {
  docker rm "$build_container"
  rm "$HERE/.build_container"
}

stop_build_container() {
  echo "${yellow}=> ${normal}Stopping build container $build_container"
  local error; error=$(docker kill "$build_container")
  if [[ $? -ne 0 ]]; then echo "$error"; exit 1; fi
  trap EXIT
}

stop_trap() {
  stop_build_container
  exit 1
}

prepare_build_container() {
  local build_image=$1; shift
  if [[ -f $HERE/.build_container ]]; then
    read -r build_container < "$HERE/.build_container"
  else
    new_build_container "$build_image"
    return
  fi

  if [[ "$clean" = true ]]; then
    echo "${yellow}=> ${normal}Removing old build container ${build_container}"
    docker kill "$build_container" || true
    docker rm "$build_container" || true
    rm "$HERE/.build_container"
  else
    local state; state=$(docker inspect --format="{{ .State.Status }}" "$build_container")
    if [[ $? -ne 0 || "$state" != "exited" ]]; then
    echo "${red}ERROR: ${normal}Build container $build_container is not in a re-usable state, is there another build running?"
    exit 1
    fi

    # was the build image updated manually with a docker pull?
    local image_id; image_id=$(docker inspect --format="{{ .Id }}" "$build_image")
    local container_image_id; container_image_id=$(docker inspect --format="{{ .Image }}" "$build_container")
    if [[ "$image_id" != "$container_image_id" ]]; then
    echo "${yellow}WARNING ${normal}Build container is not using the latest available image. Consider using '-c' to get a fresh build container using latest image"
    fi
  fi

  new_build_container "$build_image"
}

# simulate Jenkins Pipeline docker.image().inside {}
build_inside() {
  local build_image=$1; shift
  prepare_build_container "$build_image"

  trap stop_trap EXIT
  echo "${yellow}=> ${normal}Starting build container ${build_container}"
  local error; error=$(docker start "$build_container")
  if [[ $? -ne 0 ]]; then echo "$error"; exit 1; fi

  echo "${yellow}=> ${normal}Launching ${cyan}'${build_commands}'${normal}"
  echo "$build_commands" | docker exec -i -u "$(id -u)" "$build_container" /bin/bash
  stop_build_container
}

make_image() {
  echo "${yellow}=> ${normal}Building BlueOcean docker image ${tag_name}"
  (cd "$HERE" && docker build -t "$tag_name" . )
}

build_commands="mvn clean install -B -DcleanNode -Dmaven.test.failure.ignore"
tag_name=blueocean-local

usage() {
cat <<EOF
usage: $(basename $0) [-c|--clean] [-m|--make-image[=tag_name]] [-h|--help] [BUILD_COMMAND]

  Build BlueOcean plugin suite locally like it would be in Jenkins, by isolating the build
  inside a Docker container. Requires a local Docker daemon to work.
  Can also create a BlueOcean docker image if '-m' is passed.
  In order to speed up builds, the build container is kept between builds in order to keep
  Maven / NPM caches. It can be cleaned up with '-c' option.

 BUILD_COMMAND    Commands used to build BlueOcean in the build container. Defaults to "$build_commands"
 tag_name         Tag name of the build. Defaults to "$tag_name"

EOF
  exit 0
}

clean=false
make_image=false

for i in "$@"; do
    case $i in
        -h|--help)
        usage
        ;;
        -c|--clean)
        clean=true
        shift # past argument=value
        ;;
        -m=*|--make-image=*)
        make_image=true
        tag_name="${i#*=}"
        shift # past argument=value
        ;;
        -m|--make-image)
        make_image=true
        shift # past argument=value
        ;;
        *)
        break
        ;;
    esac
done

if [[ $# -ne 0 ]]; then build_commands="$*"; fi

setup_nice_output
build_inside "cloudbees/java-build-tools"
if [[ "$make_image" = true ]]; then
  make_image
fi
