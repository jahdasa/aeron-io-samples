#!/bin/sh

echo "start entrypoint.sh"
java -Djava.net.preferIPv4Stack=true -Daeron.ipc.mtu.length=8k "$@" -jar /home/aeron/jar/cluster-uber.jar
