FROM maxleap/jdk8:91
MAINTAINER Stream.Liu <xxliu@maxleap.com>

RUN \
mkdir -p /opt/shadow/lib && \
mkdir -p /opt/shadow/js && \
mkdir -p /opt/shadow/conf

ADD lib/ /opt/shadow/lib/
ADD js/ /opt/shadow/js/
ADD conf/ /opt/shadow/conf/
ADD start.sh /opt/shadow/

WORKDIR /opt/shadow/
CMD ["/opt/shadow/start.sh"]
