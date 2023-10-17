#############################################################################################
###              Stage where Docker is building spring boot app using maven               ###
#############################################################################################
FROM maven:3.8.3-openjdk-17 as build

ARG PROXY_SET=false
ARG PROXY_HOST=
ARG PROXY_PORT=

ARG MVN_PROFILES=openshift

ENV MVN_PROFILES=${MVN_PROFILES}

COPY . .

RUN mvn --batch-mode --no-transfer-progress clean package \
        -Dmaven.test.skip=true \
        -DproxySet=${PROXY_SET} \
        -DproxyHost=${PROXY_HOST} \
        -DproxyPort=${PROXY_PORT} \
        -P ${MVN_PROFILES}
#############################################################################################

#############################################################################################
### Stage where Docker is running a java process to run a service built in previous stage ###
#############################################################################################
FROM eclipse-temurin:17-jre-alpine

ARG APP_NAME
ARG APP_VERSION

ENV APP_NAME=${APP_NAME}
ENV APP_VERSION=${APP_VERSION}

WORKDIR /app

COPY --from=build "/target/${APP_NAME}-${APP_VERSION}.jar" "${APP_NAME}-${APP_VERSION}.jar"

ENTRYPOINT java -jar "${APP_NAME}-${APP_VERSION}.jar"
#############################################################################################
