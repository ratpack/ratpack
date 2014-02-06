#!/bin/bash
echo "Setting environment variables..."
export JAVA_OPTS="-Dfile.encoding=UTF-8 -server -Xmx512m -XX:+UseCompressedOops -Dratpack.port=\${PORT}"
export JAVA_HOME="\${HOME}/.jdk7"