FROM adoptopenjdk/openjdk8:alpine-slim
COPY build/libs/*SNAPSHOT.jar telnet-server.jar
ENTRYPOINT ["java", "-jar", "/telnet-server.jar"]
