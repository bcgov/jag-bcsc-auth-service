FROM eclipse-temurin:17-jre-alpine

ARG APP_NAME
ARG APP_VERSION

ENV APP_NAME=${APP_NAME}
ENV APP_VERSION=${APP_VERSION}

WORKDIR /app

COPY ./target "${APP_NAME}-${APP_VERSION}.jar"

ENTRYPOINT java -jar "${APP_NAME}-${APP_VERSION}.jar"
#############################################################################################
