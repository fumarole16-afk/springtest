FROM gradle:8.7-jdk11 AS build
WORKDIR /app
COPY build.gradle settings.gradle ./
COPY gradle/ gradle/
COPY src/ src/
RUN gradle war -x test --no-daemon

FROM tomcat:9.0-jdk11
RUN rm -rf /usr/local/tomcat/webapps/*
COPY --from=build /app/build/libs/stock-analyzer-1.0.0.war /usr/local/tomcat/webapps/ROOT.war
EXPOSE 8080
CMD ["catalina.sh", "run"]
