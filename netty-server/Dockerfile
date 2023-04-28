FROM adoptopenjdk/openjdk8:alpine-slim
COPY build/libs/*SNAPSHOT.jar netty-server.jar
ENTRYPOINT ["java", "-jar", "/netty-server.jar"]
