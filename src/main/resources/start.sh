#!/bin/bash

#JVM_OPTS="-server -Xms300M -Xmx300M -XX:+AggressiveOpts"
#JVM_OPTS="$JVM_OPTS -Xss256k"

#GC Log Options
#JVM_OPTS="$JVM_OPTS -XX:+PrintGCApplicationStoppedTime"
#JVM_OPTS="$JVM_OPTS -XX:+PrintGCTimeStamps"
#JVM_OPTS="$JVM_OPTS -XX:+PrintGCDetails"

#debug Options
#JVM_OPTS="$JVM_OPTS -Xdebug -Xrunjdwp:transport=dt_socket,address=8065,server=y,suspend=n"

MAIN_CLASS="com.maxleap.shadow.ShadowMain"
#------------------------------------------------------------------------------------------
cd `dirname $0`
BASE_DIR="`pwd -P`"
LIB_DIR=$BASE_DIR/lib
CONF_DIR=$BASE_DIR/conf

RUN_CMD="java $JVM_OPTS
-Dlog4j.configurationFile=file://${CONF_DIR}/log4j2.xml
-Dvertx.logger-delegate-factory-class-name=io.vertx.core.logging.SLF4JLogDelegateFactory
-cp ${LIB_DIR}/*: ${MAIN_CLASS}";
$RUN_CMD
