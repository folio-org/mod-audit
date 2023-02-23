FROM folioci/alpine-jre-openjdk11:latest

ENV VERTICLE_FILE mod-audit-server-fat.jar

# Set the location of the verticles
ENV VERTICLE_HOME /usr/verticles

# Copy your fat jar to the container
COPY mod-audit-server/target/${VERTICLE_FILE} ${VERTICLE_HOME}/${VERTICLE_FILE}

RUN mkdir -p jmx_exporter &&\
    wget -P jmx_exporter https://repo1.maven.org/maven2/io/prometheus/jmx/jmx_prometheus_javaagent/0.17.2/jmx_prometheus_javaagent-0.17.2.jar

COPY ./prometheus-jmx-config.yaml jmx_exporter/

# ENV JAVA_OPTIONS="$JAVA_OPTIONS -javaagent:./jmx_exporter/jmx_prometheus_javaagent-0.17.2.jar=9991:./jmx_exporter/prometheus-jmx-config.yaml"

# Expose this port locally in the container.
EXPOSE 8081 9991

CMD ['java', ' -javaagent:./jmx_exporter/jmx_prometheus_javaagent-0.17.2.jar=9991:./jmx_exporter/prometheus-jmx-config.yaml']

# exec java -XX:MaxRAMPercentage=66.0 -XX:+ExitOnOutOfMemoryError -cp . -jar /usr/verticles/mod-audit-server-fat.jar