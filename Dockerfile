FROM eclipse-temurin:17-jdk-alpine

LABEL maintainer="Vlad Vesninsky <Vlad774>"

EXPOSE 8080

ENV APP_HOME=/usr/src/app

COPY target/*.jar $APP_HOME/app.jar

WORKDIR $APP_HOME

HEALTHCHECK --interval=30s --timeout=10s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8080/actuator/health || exit 1

CMD ["java", "-jar", "app.jar"]
