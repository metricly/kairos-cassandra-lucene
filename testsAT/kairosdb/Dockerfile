ARG VERSION=1.2.1
ARG KAIROS_RELEASE_NUMBER=Metricly-v0.4

# -- build kairosdb
FROM openjdk:8

ARG VERSION
ARG KAIROS_RELEASE_NUMBER

ENV CLASSPATH=tools/tablesaw-1.2.6.jar
ENV VERSION=$VERSION
ENV KAIROS_RELEASE_NUMBER=$KAIROS_RELEASE_NUMBER

RUN mkdir /src \
	&& cd /src \
	&& git clone https://github.com/metricly/kairosdb.git \
	&& cd /src/kairosdb \
	&& git checkout master \
	&& java make package

# -- final image
FROM centos

ARG VERSION
ARG KAIROS_RELEASE_NUMBER

ENV VERSION=$VERSION
ENV KAIROS_RELEASE_NUMBER=$KAIROS_RELEASE_NUMBER

RUN yum install vim java-1.8.0-openjdk -y

COPY --from=0 /src/kairosdb/build/kairosdb-$VERSION-$KAIROS_RELEASE_NUMBER.tar.gz /opt/
RUN mkdir -p /opt/ \
	&& cd /opt/ \
	&& tar xzf kairosdb-$VERSION-$KAIROS_RELEASE_NUMBER.tar.gz -C .

# -- kairos-cassandra-lucene plugin install
COPY cassandra-lucene-0.4.jar /opt/kairosdb/lib/cassandra-lucene-0.4.jar

COPY entrypoint /entrypoint
RUN ls -lh / && chmod +x /entrypoint

EXPOSE 8080

ENTRYPOINT ["./entrypoint"]
