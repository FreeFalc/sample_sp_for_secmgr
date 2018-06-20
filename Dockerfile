FROM openjdk:8-jdk-alpine AS BUILD

ENV BUILD_APP_DIR=/usr/src/myapp

COPY ./core ${BUILD_APP_DIR}/core
COPY ./build.gradle ${BUILD_APP_DIR}
COPY ./gradlew ${BUILD_APP_DIR}
COPY ./settings.gradle ${BUILD_APP_DIR}
COPY ./gradle.properties ${BUILD_APP_DIR}
COPY ./samples ${BUILD_APP_DIR}/samples
COPY ./src ${BUILD_APP_DIR}/src
COPY ./gradle ${BUILD_APP_DIR}/gradle


RUN apk update && apk add bash
RUN cd ${BUILD_APP_DIR} \
	&& ./gradlew -b ./samples/boot/simple-service-provider/build.gradle bootJar


FROM openjdk:8-jre-alpine

ENV APP_NAME=simple-service-provider-2.0.0.BUILD-SNAPSHOT.jar

COPY --from=BUILD /usr/src/myapp/samples/boot/simple-service-provider/build/libs/spring-security-saml-samples/boot/${APP_NAME} /opt/

EXPOSE 8088

CMD ["sh", "-c", "java -jar /opt/${APP_NAME}"]


