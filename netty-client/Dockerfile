FROM adoptopenjdk/openjdk8:alpine-slim
COPY build/libs/*SNAPSHOT.jar netty-client.jar
ENTRYPOINT ["java", "-jar", "/netty-client.jar"]
