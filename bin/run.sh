#!/usr/bin/env bash

cd ..
BASE_DIR=.

JAVA_EXE=java
JAVA_OPT="-Xmx256m -Xms64m -Djava.security.egd=file:///dev/urandom -Dname=tool"
MAIN_CLASS=com.nullpo.Application

for f in ${BASE_DIR}/lib/*.jar;
do
  CLASS_PATH=${CLASS_PATH}:$f
done

${JAVA_EXE} ${JAVA_OPT} -classpath ${CLASS_PATH} ${MAIN_CLASS} "$@"