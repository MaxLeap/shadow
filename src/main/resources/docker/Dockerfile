FROM maxleap/jdk8:91
MAINTAINER Stream.Liu <xxliu@maxleap.com>

RUN \
mkdir -p /opt/tools/shadow/lib && \
mkdir -p /opt/tools/shadow/conf

ADD lib/ /opt/tools/shadow/lib/
ADD log4j2.xml /opt/tools/shadow/conf/
ADD start.sh /opt/tools/shadow/

WORKDIR /opt/tools/shadow/
ENTRYPOINT ["./start.sh"]
