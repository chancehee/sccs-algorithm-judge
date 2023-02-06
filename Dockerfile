FROM openjdk:8-jdk-alpine
ARG JAR_FILE_PATH=build/libs/Judge-0.0.1-SNAPSHOT.jar
COPY $JAR_FILE_PATH app.jar
ENTRYPOINT ["java","-jar","/app.jar"]