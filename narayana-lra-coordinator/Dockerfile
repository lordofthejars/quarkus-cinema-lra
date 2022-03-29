FROM registry.access.redhat.com/ubi8/openjdk-11:1.11

ENV LANG='en_US.UTF-8' LANGUAGE='en_US:en'


COPY --chown=185 lra-coordinator-runner.jar /deployments/
COPY --chown=185 lra-coordinator.jar /deployments/

EXPOSE 8080
USER 185
ENV AB_JOLOKIA_OFF=""
ENV JAVA_APP_JAR="/deployments/lra-coordinator-runner.jar"
