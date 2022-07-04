#!/bin/bash

repl(){
  clj \
    -J-Dclojure.core.async.pool-size=8 \
    -X:Ripley Ripley.core/process \
    :main-ns Ahsoka-Tano.main
}


main(){
  clojure \
    -J-Dclojure.core.async.pool-size=8 \
    -M -m Ahsoka-Tano.main
}

jar(){

  rm -rf out/*.jar out/classes
  clojure \
    -X:Genie Genie.core/process \
    :main-ns Ahsoka-Tano.main \
    :filename "\"out/Ahsoka-Tano-$(git rev-parse --short HEAD).jar\"" \
    :paths '["src"]'
}

release(){
  jar
}

"$@"