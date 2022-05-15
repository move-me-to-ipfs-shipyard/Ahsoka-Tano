#!/bin/bash

repl(){
  clj \
    -J-Dclojure.core.async.pool-size=1 \
    -X:Ripley Ripley.core/process \
    :main-ns Ahsoka-Tano.main
}


main(){
  clojure \
    -J-Dclojure.core.async.pool-size=1 \
    -M -m Ahsoka-Tano.main
}

tag(){
  COMMIT_HASH=$(git rev-parse --short HEAD)
  COMMIT_COUNT=$(git rev-list --count HEAD)
  TAG="$COMMIT_COUNT-$COMMIT_HASH"
  git tag $TAG $COMMIT_HASH
  echo $COMMIT_HASH
  echo $TAG
}

jar(){

  clojure \
    -X:Zazu Zazu.core/process \
    :word '"Ahsoka-Tano"' \
    :filename '"out/identicon/icon.png"' \
    :size 256

  rm -rf out/*.jar
  COMMIT_HASH=$(git rev-parse --short HEAD)
  COMMIT_COUNT=$(git rev-list --count HEAD)
  clojure \
    -X:Genie Genie.core/process \
    :main-ns Ahsoka-Tano.main \
    :filename "\"out/Ahsoka-Tano-$COMMIT_COUNT-$COMMIT_HASH.jar\"" \
    :paths '["src" "out/identicon"]'
}

release(){
  jar
}

"$@"